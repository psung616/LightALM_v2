package com.lightalm.formlayout.dto;

import com.lightalm.formlayout.domain.FieldSource;
import jakarta.validation.constraints.NotNull;

public record SaveFormLayoutFieldRequest(
        @NotNull(message = "fieldSource는 필수입니다.") FieldSource fieldSource,
        String standardFieldKey,
        Long customFieldId,
        Integer displayOrder,
        Boolean visible
) {
}
