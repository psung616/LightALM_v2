package com.lightalm.review.dto;

import com.lightalm.review.domain.ReviewDecision;
import jakarta.validation.constraints.NotNull;

/** 04-api.md §4.17 {@code PATCH .../review-cycles/{cycleId}/participants/me}. PENDING은 서비스에서 거부한다. */
public record RecordReviewDecisionRequest(
        @NotNull(message = "decision은 필수입니다.") ReviewDecision decision,
        String comment) {
}
