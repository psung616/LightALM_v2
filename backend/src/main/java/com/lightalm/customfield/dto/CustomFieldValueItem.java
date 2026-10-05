package com.lightalm.customfield.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ADR-012 §A.4 PUT .../custom-field-values 요청 바디의 배열 원소.
 * value가 null/빈 문자열이면 해당 필드 값을 비운다(필수 필드 검증은 서비스 레이어에서 수행).
 */
public record CustomFieldValueItem(
        @NotNull(message = "fieldId는 필수입니다.") Long fieldId,
        String value
) {
}
