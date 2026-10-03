package com.lightalm.license.domain;

/**
 * ADR-011 §2.2: 라이센스 타입. 이 설계에서는 메타데이터로만 저장하고
 * 기능 차등 적용(feature gating)은 하지 않는다.
 */
public enum LicenseType {
    TRIAL,
    STANDARD,
    ENTERPRISE
}
