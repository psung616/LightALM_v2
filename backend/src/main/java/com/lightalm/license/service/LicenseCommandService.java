package com.lightalm.license.service;

import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseDescriptor;
import com.lightalm.license.domain.LicenseFileRejectedException;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.dto.LicenseUploadResponse;
import com.lightalm.license.repository.LicenseRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.AuditLogService;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ADR-011 §2.3: 라이센스 파일 업로드 → 파싱 → 서명/만료 검증 → 적용(기존 ACTIVE superseded, 신규 ACTIVE 삽입).
 */
@Service
@RequiredArgsConstructor
public class LicenseCommandService {

    private final LicenseRepository licenseRepository;
    private final LicenseFileParser licenseFileParser;
    private final LicenseSignatureVerifier licenseSignatureVerifier;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public LicenseUploadResponse upload(MultipartFile file, UserPrincipal principal) {
        byte[] content = readBytes(file);
        LicenseDescriptor descriptor = licenseFileParser.parse(content);

        if (!licenseSignatureVerifier.isValid(descriptor)) {
            throw new LicenseFileRejectedException("LICENSE_SIGNATURE_INVALID", "라이센스 서명이 일치하지 않습니다.");
        }
        if (descriptor.seatLimit() == null || descriptor.seatLimit() <= 0) {
            throw new ValidationException("seatLimit은 1 이상이어야 합니다.");
        }
        Instant expiresAtInstant = descriptor.expiresAtInstant();
        if (expiresAtInstant != null && expiresAtInstant.isBefore(Instant.now())) {
            throw new LicenseFileRejectedException("LICENSE_ALREADY_EXPIRED", "이미 만료된 라이센스 파일입니다.");
        }
        Instant issuedAtInstant = descriptor.issuedAtInstant();
        if (issuedAtInstant == null) {
            throw new ValidationException("issuedAt은 필수입니다.");
        }

        // supersede()는 더티 체킹에 의한 UPDATE라 flush 시점까지 DB에 반영되지 않는다.
        // 아래 신규 License는 IDENTITY 생성 전략이라 save()가 즉시 INSERT를 실행하므로,
        // flush를 호출하지 않으면 INSERT(새 ACTIVE)가 UPDATE(기존 ACTIVE→SUPERSEDED)보다
        // 먼저 DB에 도달해 uq_licenses_single_active 유니크 제약을 위반한다(qa-tester 발견).
        licenseRepository.findByStatus(LicenseStatus.ACTIVE).ifPresent(existing -> {
            existing.supersede();
            licenseRepository.saveAndFlush(existing);
        });

        User uploadedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        License license = License.builder()
                .licenseKey(descriptor.licenseKey())
                .organizationName(descriptor.organizationName())
                .licenseType(descriptor.licenseType())
                .seatLimit(descriptor.seatLimit())
                .issuedAt(toLocalDateTime(issuedAtInstant))
                .expiresAt(expiresAtInstant != null ? toLocalDateTime(expiresAtInstant) : null)
                .rawFileName(file.getOriginalFilename())
                .rawPayload(new String(content, StandardCharsets.UTF_8))
                .signatureValid(true)
                .uploadedBy(uploadedBy)
                .build();

        License saved = licenseRepository.save(license);

        auditLogService.record(null, AuditTargetType.LICENSE, saved.getId(), AuditAction.CREATE,
                null, null, saved.getLicenseKey(), principal.getId());

        long seatsUsed = userRepository.countByEnabledTrue();
        return LicenseUploadResponse.from(saved, seatsUsed);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ValidationException("업로드된 파일을 읽을 수 없습니다.");
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
