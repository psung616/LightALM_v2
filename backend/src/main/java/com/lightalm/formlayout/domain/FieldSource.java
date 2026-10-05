package com.lightalm.formlayout.domain;

/**
 * ADR-012 §B.2. form_layout_fields.field_source — 표준 필드를 배치한 것인지, 커스텀 필드를
 * 배치한 것인지 구분한다.
 */
public enum FieldSource {
    STANDARD,
    CUSTOM
}
