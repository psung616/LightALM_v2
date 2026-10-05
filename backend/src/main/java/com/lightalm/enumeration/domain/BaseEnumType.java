package com.lightalm.enumeration.domain;

/**
 * ADR-012 §C.2. {@code project_enumeration_sets.base_enum}이 기존 고정 Java enum 중
 * 무엇을 확장하는지 나타낸다. 전부 정의해두지만, {@code EnumerationSetService}는
 * {@code PRIORITY} 또는 {@code null}(커스텀 필드 전용)만 집합 생성을 허용한다 — §C.1의
 * 명시적 제약(상태값 자체의 확장은 이번 범위 밖).
 */
public enum BaseEnumType {
    PRIORITY,
    REQUIREMENT_STATUS,
    ISSUE_STATUS,
    TEST_CASE_STATUS
}
