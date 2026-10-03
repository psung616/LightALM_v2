package com.lightalm.license.dto;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.domain.LicenseType;
import java.time.LocalDateTime;

/**
 * ADR-011 §2.3 7단계: 업로드 성공 직후 응답(저장된 라이센스 상세 + 현재 시트 사용량).
 */
public record LicenseUploadResponse(
        Long id,
        String licenseKey,
        String organizationName,
        LicenseType licenseType,
        LicenseStatus status,
        Integer seatLimit,
        long seatsUsed,
        long seatsRemaining,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {

    public static LicenseUploadResponse from(License license, long seatsUsed) {
        long remaining = Math.max(0, license.getSeatLimit() - seatsUsed);
        return new LicenseUploadResponse(
                license.getId(),
                license.getLicenseKey(),
                license.getOrganizationName(),
                license.getLicenseType(),
                license.getStatus(),
                license.getSeatLimit(),
                seatsUsed,
                remaining,
                license.getIssuedAt(),
                license.getExpiresAt());
    }
}
