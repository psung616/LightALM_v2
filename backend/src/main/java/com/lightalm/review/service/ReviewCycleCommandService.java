package com.lightalm.review.service;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.review.domain.ReviewCycle;
import com.lightalm.review.domain.ReviewDecision;
import com.lightalm.review.domain.ReviewParticipant;
import com.lightalm.review.dto.CreateReviewCycleRequest;
import com.lightalm.review.dto.RecordReviewDecisionRequest;
import com.lightalm.review.dto.ReviewCycleDetailResponse;
import com.lightalm.review.repository.ReviewCycleRepository;
import com.lightalm.review.repository.ReviewParticipantRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-008(Phase 16) / 04-api.md §4.17. 리뷰 사이클 생성·참여자 결정 기록·닫기.
 *
 * <p><b>가장 중요한 제약</b>: 이 서비스의 어떤 메서드도 대상(요구사항/이슈)의 status를 변경하지
 * 않는다. 참여자 결정({@link #recordMyDecision})과 사이클 닫기({@link #close}) 모두 기록·표시
 * 용도일 뿐이다(03-data-model.md §3.19, 01-scope.md §1.3 원칙). 이를 구조적으로 보장하기 위해
 * 요구사항/이슈/승인/워크플로우 관련 Repository·Service를 의존성으로 두지 않는다
 * ({@code ReviewCycleCommandServiceTest}가 검사).</p>
 */
@Service
@RequiredArgsConstructor
public class ReviewCycleCommandService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final ReviewParticipantRepository reviewParticipantRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final PolymorphicTargetValidator polymorphicTargetValidator;

    @Transactional
    public ReviewCycleDetailResponse create(Long projectId, TargetType targetType, Long targetId,
                                            CreateReviewCycleRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        ReviewCycleTargetTypePolicy.requireSupported(targetType);
        polymorphicTargetValidator.ensureExists(projectId, targetType, targetId);

        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        Set<Long> participantIds = new LinkedHashSet<>(request.participantUserIds());
        List<User> participantUsers = userRepository.findAllById(participantIds);
        if (participantUsers.size() != participantIds.size()) {
            throw new ValidationException("존재하지 않는 참여자 사용자 ID가 포함되어 있습니다.");
        }
        // 프로젝트 멤버가 아닌 사용자는 이후 결정 기록 API(VIEWER+)에 접근할 수 없어 영원히 PENDING으로 남으므로
        // 생성 시점에 거부한다. 시스템 ADMIN은 requireRole이 모든 프로젝트에서 통과시키므로 예외로 허용한다.
        for (User user : participantUsers) {
            boolean isMember = projectMemberService.findRole(projectId, user.getId()).isPresent();
            if (!isMember && user.getSystemRole() != SystemRole.ADMIN) {
                throw new ValidationException("프로젝트 멤버가 아닌 사용자는 리뷰 참여자로 지정할 수 없습니다: " + user.getId());
            }
        }

        ReviewCycle cycle = ReviewCycle.builder()
                .project(project)
                .targetType(targetType)
                .targetId(targetId)
                .name(request.name())
                .createdBy(creator)
                .build();
        reviewCycleRepository.save(cycle);

        List<ReviewParticipant> participants = participantUsers.stream()
                .map(user -> ReviewParticipant.builder().reviewCycle(cycle).user(user).build())
                .toList();
        reviewParticipantRepository.saveAll(participants);

        return ReviewCycleDetailResponse.from(cycle, participants);
    }

    /**
     * 본인의 결정을 기록한다. 해당 사이클의 참여자가 아니면 역할과 무관하게 거부한다
     * (04-api.md §4.17 "해당 사이클의 participant 본인만"). 닫힌 사이클과 PENDING 값은 거부한다.
     */
    @Transactional
    public ReviewCycleDetailResponse recordMyDecision(Long projectId, Long cycleId, RecordReviewDecisionRequest request,
                                                      UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        ReviewCycle cycle = getCycleEntity(projectId, cycleId);
        ReviewParticipant participant = reviewParticipantRepository.findByReviewCycleIdAndUserId(cycleId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("해당 리뷰 사이클의 참여자가 아닙니다."));
        if (!cycle.isOpen()) {
            throw new ValidationException("닫힌 리뷰 사이클에는 결정을 기록할 수 없습니다.");
        }
        if (request.decision() == ReviewDecision.PENDING) {
            throw new ValidationException("decision은 APPROVE, REJECT, COMMENT_ONLY 중 하나여야 합니다.");
        }

        participant.recordDecision(request.decision(), request.comment());

        return ReviewCycleDetailResponse.from(cycle, reviewParticipantRepository.findByReviewCycleIdOrderById(cycleId));
    }

    /** 사이클을 닫는다. 대상의 status는 전혀 건드리지 않는다(클래스 주석 참고). */
    @Transactional
    public ReviewCycleDetailResponse close(Long projectId, Long cycleId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        ReviewCycle cycle = getCycleEntity(projectId, cycleId);
        if (!cycle.isOpen()) {
            throw new ValidationException("이미 닫힌 리뷰 사이클입니다.");
        }
        cycle.close();
        return ReviewCycleDetailResponse.from(cycle, reviewParticipantRepository.findByReviewCycleIdOrderById(cycleId));
    }

    private ReviewCycle getCycleEntity(Long projectId, Long cycleId) {
        return reviewCycleRepository.findByIdAndProjectId(cycleId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("리뷰 사이클을 찾을 수 없습니다: " + cycleId));
    }
}
