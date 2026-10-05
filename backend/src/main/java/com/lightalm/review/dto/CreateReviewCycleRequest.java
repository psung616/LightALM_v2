package com.lightalm.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 04-api.md §4.17 {@code POST /api/projects/{projectId}/{targetType}/{targetId}/review-cycles}. */
public record CreateReviewCycleRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 150, message = "name은 150자 이하여야 합니다.")
        String name,
        @NotEmpty(message = "participantUserIds는 최소 1명 이상이어야 합니다.")
        List<@NotNull Long> participantUserIds) {
}
