package com.lightalm.review.service;

import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.review.dto.ReviewCycleDetailResponse;
import com.lightalm.review.repository.ReviewCycleRepository;
import com.lightalm.review.repository.ReviewParticipantRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADR-008(Phase 16) / 04-api.md §4.17. 대상(요구사항/이슈)의 리뷰 사이클 목록(참여자 결정 현황 포함). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewCycleQueryService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewParticipantRepository reviewParticipantRepository;
    private final ProjectMemberService projectMemberService;
    private final PolymorphicTargetValidator polymorphicTargetValidator;

    public List<ReviewCycleDetailResponse> listForTarget(Long projectId, TargetType targetType, Long targetId,
                                                         UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        ReviewCycleTargetTypePolicy.requireSupported(targetType);
        polymorphicTargetValidator.ensureExists(projectId, targetType, targetId);

        return reviewCycleRepository.findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(projectId, targetType, targetId)
                .stream()
                .map(cycle -> ReviewCycleDetailResponse.from(cycle,
                        reviewParticipantRepository.findByReviewCycleIdOrderById(cycle.getId())))
                .toList();
    }
}
