package com.lightalm.customfield.domain;

/**
 * ADR-012 §A.3. 하드 삭제는 없다 — DEPRECATED는 소프트 비활성 상태다.
 */
public enum CustomFieldStatus {
    ACTIVE,
    DEPRECATED
}
