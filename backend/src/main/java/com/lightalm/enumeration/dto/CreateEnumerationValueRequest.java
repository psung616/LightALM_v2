package com.lightalm.enumeration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ADR-012 §C.4 값 추가 요청 바디. */
public record CreateEnumerationValueRequest(
        @NotBlank(message = "valueKey는 필수입니다.") @Size(max = 50) String valueKey,
        @NotBlank(message = "label은 필수입니다.") @Size(max = 100) String label,
        Integer displayOrder
) {
}
