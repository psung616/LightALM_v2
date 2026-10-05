package com.lightalm.customfield.dto;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;

/**
 * ADR-012 §A.4 GET .../custom-field-values 응답 원소. 대상(target) 하나에 딸린 필드 값 전체를
 * 리스트로 내려주는 용도라 별도의 Summary/Detail 구분이 의미가 없다(필드 1개=값 1개, 중첩 없음) —
 * 이 DTO 하나로 통일한다.
 */
public record CustomFieldValueResponse(
        Long fieldId,
        String fieldKey,
        String label,
        CustomFieldDataType dataType,
        Boolean required,
        CustomFieldStatus status,
        String value
) {

    public static CustomFieldValueResponse of(CustomFieldDefinition definition, String value) {
        return new CustomFieldValueResponse(
                definition.getId(),
                definition.getFieldKey(),
                definition.getLabel(),
                definition.getDataType(),
                definition.getRequired(),
                definition.getStatus(),
                value);
    }
}
