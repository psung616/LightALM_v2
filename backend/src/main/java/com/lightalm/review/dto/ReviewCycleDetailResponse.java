package com.lightalm.review.dto;

import com.lightalm.domain.TargetType;
import com.lightalm.review.domain.ReviewCycle;
import com.lightalm.review.domain.ReviewCycleStatus;
import com.lightalm.review.domain.ReviewParticipant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 리뷰 사이클 1건 + 참여자 결정 현황. 04-api.md §4.17의 목록 API가 "참여자 결정 현황 포함"을 요구하고
 * 단건 조회 API는 따로 없으므로, 목록 원소와 생성/결정/닫기 응답이 모두 이 Detail 형태를 쓴다
 * (참여자를 뺀 Summary 형태를 쓰는 화면이 없어 별도 Summary DTO를 두지 않음).
 */
public record ReviewCycleDetailResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        String name,
        ReviewCycleStatus status,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<ReviewParticipantResponse> participants) {

    public static ReviewCycleDetailResponse from(ReviewCycle cycle, List<ReviewParticipant> participants) {
        return new ReviewCycleDetailResponse(
                cycle.getId(),
                cycle.getTargetType(),
                cycle.getTargetId(),
                cycle.getName(),
                cycle.getStatus(),
                cycle.getCreatedBy() != null ? cycle.getCreatedBy().getId() : null,
                cycle.getCreatedBy() != null ? cycle.getCreatedBy().getFullName() : null,
                cycle.getCreatedAt(),
                cycle.getClosedAt(),
                participants.stream().map(ReviewParticipantResponse::from).toList());
    }
}
