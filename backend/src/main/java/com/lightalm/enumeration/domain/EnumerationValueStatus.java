package com.lightalm.enumeration.domain;

/**
 * ADR-012 §C.2. 하드 삭제는 없다 — DEPRECATED는 소프트 비활성 상태다(CustomFieldStatus와 동일 패턴).
 */
public enum EnumerationValueStatus {
    ACTIVE,
    DEPRECATED
}
