package com.lightalm.license.domain;

import java.time.Instant;

/**
 * 업로드된 "Light ALM 라이센스 디스크립터" 파일(JSON)을 파싱한 결과를 담는 값 객체(ADR-011 §2.2).
 * issuedAt/expiresAt은 서명(HMAC) 재계산 시 원본 파일의 문자열 그대로를 canonical 문자열에 사용해야
 * 하므로(서명 발급 측과 동일한 표현이어야 재계산이 일치한다) String으로 보관하고,
 * 날짜 연산이 필요한 곳에서만 Instant로 변환해 사용한다.
 */
public record LicenseDescriptor(
        String licenseKey,
        String organizationName,
        LicenseType licenseType,
        Integer seatLimit,
        String issuedAt,
        String expiresAt,
        String signature
) {

    public Instant issuedAtInstant() {
        return issuedAt == null ? null : Instant.parse(issuedAt);
    }

    public Instant expiresAtInstant() {
        return (expiresAt == null || expiresAt.isBlank()) ? null : Instant.parse(expiresAt);
    }

    /**
     * 서명 검증에 사용하는 canonical 문자열(ADR-011 §2.2):
     * licenseKey\norganizationName\nlicenseType\nseatLimit\nissuedAt\nexpiresAt
     * expiresAt이 없는(무기한) 라이센스는 빈 문자열로 취급한다.
     */
    public String toCanonicalString() {
        return String.join("\n",
                licenseKey == null ? "" : licenseKey,
                organizationName == null ? "" : organizationName,
                licenseType == null ? "" : licenseType.name(),
                seatLimit == null ? "" : String.valueOf(seatLimit),
                issuedAt == null ? "" : issuedAt,
                expiresAt == null ? "" : expiresAt);
    }
}
