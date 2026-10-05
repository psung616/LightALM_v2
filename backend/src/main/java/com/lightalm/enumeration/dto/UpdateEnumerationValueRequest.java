package com.lightalm.enumeration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ADR-012 §C.4 값 수정 요청 바디 — label/displayOrder만 수정 가능(valueKey 불변). */
public record UpdateEnumerationValueRequest(
        @NotBlank(message = "label은 필수입니다.") @Size(max = 100) String label,
        Integer displayOrder
) {
}
