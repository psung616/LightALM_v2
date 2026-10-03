package com.lightalm.license.dto;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.domain.LicenseType;
import java.time.LocalDateTime;

/**
 * 현재 활성 라이센스 상세(/api/admin/licenses/current)용 응답. 시트 사용량을 함께 내려준다.
 */
public record LicenseDetailResponse(
        Long id,
        String licenseKey,
        String organizationName,
        LicenseType licenseType,
        LicenseStatus status,
        Integer seatLimit,
        long seatsUsed,
        long seatsRemaining,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        String rawFileName,
        String uploadedByUsername,
        LocalDateTime uploadedAt
) {

    public static LicenseDetailResponse from(License license, long seatsUsed) {
        long remaining = Math.max(0, license.getSeatLimit() - seatsUsed);
        return new LicenseDetailResponse(
                license.getId(),
                license.getLicenseKey(),
                license.getOrganizationName(),
                license.getLicenseType(),
                license.getStatus(),
                license.getSeatLimit(),
                seatsUsed,
                remaining,
                license.getIssuedAt(),
                license.getExpiresAt(),
                license.getRawFileName(),
                license.getUploadedBy() != null ? license.getUploadedBy().getUsername() : null,
                license.getUploadedAt());
    }
}
