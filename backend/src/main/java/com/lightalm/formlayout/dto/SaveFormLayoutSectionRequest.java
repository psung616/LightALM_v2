package com.lightalm.formlayout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveFormLayoutSectionRequest(
        @NotBlank(message = "title은 필수입니다.") @Size(max = 100) String title,
        Integer displayOrder,
        @NotNull(message = "fields는 필수입니다.") @Valid List<SaveFormLayoutFieldRequest> fields
) {
}
