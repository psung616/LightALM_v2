package com.lightalm.baseline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 04-api.md §4.18 {@code POST /api/projects/{projectId}/baselines}. */
public record CreateBaselineRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 150, message = "name은 150자 이하여야 합니다.")
        String name,
        String description,
        @NotEmpty(message = "itemRefs는 최소 1개 이상이어야 합니다.")
        List<@Valid @NotNull BaselineItemRef> itemRefs) {
}
