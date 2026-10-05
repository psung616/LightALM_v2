package com.lightalm.customfield.dto;

import com.lightalm.customfield.domain.CustomFieldDataType;
import com.lightalm.domain.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ADR-012 §A.4 POST 요청 바디. fieldKey는 영숫자/언더바 식별자로 한정한다(§A.3).
 */
public record CreateCustomFieldDefinitionRequest(
        @NotNull(message = "targetType은 필수입니다.") TargetType targetType,
        @NotBlank(message = "fieldKey는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,49}$", message = "fieldKey는 영문으로 시작하는 영숫자/언더바 조합이어야 합니다.")
        String fieldKey,
        @NotBlank(message = "label은 필수입니다.") @Size(max = 100) String label,
        @NotNull(message = "dataType은 필수입니다.") CustomFieldDataType dataType,
        Long enumerationSetId,
        Boolean required,
        String defaultValue
) {
}
