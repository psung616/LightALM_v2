package com.lightalm.customfield.domain;

/**
 * ADR-012 §A.1. 지원 데이터 타입 6종. SINGLE_SELECT/MULTI_SELECT는 Phase 22(열거형)의
 * 열거형 집합을 선택지로 참조한다. enumerationSetId는 생성 시 같은 프로젝트 소속 집합인지
 * 애플리케이션 레벨에서 검증한다(qa-tester 권고 반영, 2026-10-05 — ADR-012 §A 각주).
 */
public enum CustomFieldDataType {
    TEXT,
    NUMBER,
    DATE,
    BOOLEAN,
    SINGLE_SELECT,
    MULTI_SELECT
}
