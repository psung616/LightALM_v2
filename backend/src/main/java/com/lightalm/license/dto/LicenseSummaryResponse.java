package com.lightalm.license.dto;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.domain.LicenseType;
import java.time.LocalDateTime;

/**
 * 업로드 이력 목록(/admin/licenses 화면 테이블)용 응답. 상세 화면은 {@link LicenseDetailResponse}를 쓴다.
 */
public record LicenseSummaryResponse(
        Long id,
        String licenseKey,
        String organizationName,
        LicenseType licenseType,
        LicenseStatus status,
        Integer seatLimit,
        String rawFileName,
        String uploadedByUsername,
        LocalDateTime uploadedAt
) {

    public static LicenseSummaryResponse from(License license) {
        return new LicenseSummaryResponse(
                license.getId(),
                license.getLicenseKey(),
                license.getOrganizationName(),
                license.getLicenseType(),
                license.getStatus(),
                license.getSeatLimit(),
                license.getRawFileName(),
                license.getUploadedBy() != null ? license.getUploadedBy().getUsername() : null,
                license.getUploadedAt());
    }
}
