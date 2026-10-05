package com.lightalm.customfield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ADR-012 §A.4 PUT 요청 바디. dataType/fieldKey는 포함하지 않는다(생성 후 불변).
 */
public record UpdateCustomFieldDefinitionRequest(
        @NotBlank(message = "label은 필수입니다.") @Size(max = 100) String label,
        Boolean required,
        String defaultValue,
        Integer displayOrder
) {
}
