package com.lightalm.customfield.dto;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.domain.TargetType;

/**
 * 폼 렌더링용 활성 필드 목록(GET /custom-fields, VIEWER+)에서 쓰는 축약 응답.
 * 설정 화면의 전체 목록(상태/생성정보 포함)은 {@link CustomFieldDefinitionDetailResponse}를 쓴다.
 */
public record CustomFieldDefinitionSummaryResponse(
        Long id,
        TargetType targetType,
        String fieldKey,
        String label,
        CustomFieldDataType dataType,
        Long enumerationSetId,
        Boolean required,
        String defaultValue,
        Integer displayOrder
) {

    public static CustomFieldDefinitionSummaryResponse from(CustomFieldDefinition definition) {
        return new CustomFieldDefinitionSummaryResponse(
                definition.getId(),
                definition.getTargetType(),
                definition.getFieldKey(),
                definition.getLabel(),
                definition.getDataType(),
                definition.getEnumerationSetId(),
                definition.getRequired(),
                definition.getDefaultValue(),
                definition.getDisplayOrder());
    }
}
