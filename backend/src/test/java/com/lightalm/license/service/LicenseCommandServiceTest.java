package com.lightalm.license.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseDescriptor;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.domain.LicenseType;
import com.lightalm.license.repository.LicenseRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * qa-tester REJECT 회귀 테스트: 두 번째 라이센스 업로드 시
 * 기존 ACTIVE 라이센스의 supersede()가 flush되지 않은 채 신규 ACTIVE License를
 * save()하면 uq_licenses_single_active 유니크 제약을 위반한다(500).
 * LicenseCommandService.upload()는 supersede 후 반드시 flush를 먼저 수행해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class LicenseCommandServiceTest {

    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private LicenseFileParser licenseFileParser;
    @Mock
    private LicenseSignatureVerifier licenseSignatureVerifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LicenseCommandService licenseCommandService;

    private UserPrincipal adminPrincipal;
    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .username("admin")
                .password("hash")
                .email("admin@example.com")
                .fullName("Admin")
                .systemRole(SystemRole.ADMIN)
                .enabled(true)
                .build();
        adminPrincipal = new UserPrincipal(admin);
    }

    private LicenseDescriptor validDescriptor(String licenseKey) {
        return new LicenseDescriptor(
                licenseKey, "ACME Corp", LicenseType.STANDARD, 50,
                "2026-01-01T00:00:00Z", "2027-01-01T00:00:00Z", "valid-signature");
    }

    private License existingActiveLicense() {
        return License.builder()
                .licenseKey("LALM-LIC-OLD")
                .organizationName("ACME Corp")
                .licenseType(LicenseType.STANDARD)
                .seatLimit(50)
                .issuedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .rawFileName("old.json")
                .rawPayload("{}")
                .signatureValid(true)
                .build();
    }

    @Test
    void upload_flushesSupersededExistingLicense_beforeInsertingNewActiveLicense() {
        License existing = existingActiveLicense();
        MockMultipartFile file = new MockMultipartFile("file", "new.json", "application/json",
                "{}".getBytes());

        when(licenseFileParser.parse(any(byte[].class))).thenReturn(validDescriptor("LALM-LIC-NEW"));
        when(licenseSignatureVerifier.isValid(any(LicenseDescriptor.class))).thenReturn(true);
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(existing));
        when(licenseRepository.saveAndFlush(existing)).thenReturn(existing);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(licenseRepository.save(any(License.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByEnabledTrue()).thenReturn(1L);

        licenseCommandService.upload(file, adminPrincipal);

        assertThat(existing.getStatus()).isEqualTo(LicenseStatus.SUPERSEDED);

        // 핵심 회귀 검증: 기존 라이센스의 supersede()가 flush(saveAndFlush)된 다음에만
        // 새 License의 save()가 호출되어야 한다. 순서가 뒤바뀌면 운영 DB에서
        // uq_licenses_single_active 유니크 제약을 위반해 500이 발생한다.
        InOrder inOrder = inOrder(licenseRepository);
        inOrder.verify(licenseRepository).saveAndFlush(existing);
        inOrder.verify(licenseRepository).save(any(License.class));
    }

    @Test
    void upload_firstLicense_savesWithoutSupersedingAnything() {
        MockMultipartFile file = new MockMultipartFile("file", "first.json", "application/json",
                "{}".getBytes());

        when(licenseFileParser.parse(any(byte[].class))).thenReturn(validDescriptor("LALM-LIC-FIRST"));
        when(licenseSignatureVerifier.isValid(any(LicenseDescriptor.class))).thenReturn(true);
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(licenseRepository.save(any(License.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByEnabledTrue()).thenReturn(1L);

        var response = licenseCommandService.upload(file, adminPrincipal);

        assertThat(response.licenseKey()).isEqualTo("LALM-LIC-FIRST");
        assertThat(response.status()).isEqualTo(LicenseStatus.ACTIVE);
    }
}
