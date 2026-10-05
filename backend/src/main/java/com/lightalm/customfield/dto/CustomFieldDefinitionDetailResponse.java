package com.lightalm.customfield.dto;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.customfield.domain.CustomFieldDefinition;
import com.lightalm.customfield.domain.CustomFieldStatus;
import com.lightalm.domain.TargetType;
import java.time.LocalDateTime;

/**
 * 설정 화면(GET /config/custom-fields, PROJECT_ADMIN+)용 상세 응답 — status/생성정보를 포함한다.
 */
public record CustomFieldDefinitionDetailResponse(
        Long id,
        TargetType targetType,
        String fieldKey,
        String label,
        CustomFieldDataType dataType,
        Long enumerationSetId,
        Boolean required,
        String defaultValue,
        Integer displayOrder,
        CustomFieldStatus status,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CustomFieldDefinitionDetailResponse from(CustomFieldDefinition definition) {
        return new CustomFieldDefinitionDetailResponse(
                definition.getId(),
                definition.getTargetType(),
                definition.getFieldKey(),
                definition.getLabel(),
                definition.getDataType(),
                definition.getEnumerationSetId(),
                definition.getRequired(),
                definition.getDefaultValue(),
                definition.getDisplayOrder(),
                definition.getStatus(),
                definition.getCreatedBy() != null ? definition.getCreatedBy().getUsername() : null,
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }
}
