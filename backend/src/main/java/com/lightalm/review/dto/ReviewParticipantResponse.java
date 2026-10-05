package com.lightalm.review.dto;

import com.lightalm.review.domain.ReviewDecision;
import com.lightalm.review.domain.ReviewParticipant;
import java.time.LocalDateTime;

/** {@link ReviewCycleDetailResponse#participants()}의 원소 — 참여자 1명의 결정 현황. */
public record ReviewParticipantResponse(
        Long id,
        Long userId,
        String username,
        String fullName,
        ReviewDecision decision,
        String comment,
        LocalDateTime decidedAt) {

    public static ReviewParticipantResponse from(ReviewParticipant participant) {
        return new ReviewParticipantResponse(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getUsername(),
                participant.getUser().getFullName(),
                participant.getDecision(),
                participant.getComment(),
                participant.getDecidedAt());
    }
}
