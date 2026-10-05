package com.lightalm.workflow.service;

import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import com.lightalm.workflow.dto.CreateWorkflowTransitionRuleRequest;
import com.lightalm.workflow.dto.NextStatusesResponse;
import com.lightalm.workflow.dto.WorkflowRuleListResponse;
import com.lightalm.workflow.dto.WorkflowTransitionRuleResponse;
import com.lightalm.workflow.repository.WorkflowTransitionRuleRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §D.4. 워크플로우 전이 규칙 설정화면(PROJECT_ADMIN+) CRUD와 폼 렌더링용 다음 상태
 * 조회(VIEWER+). 실제 전이 허용/거부 판단은 {@link WorkflowTransitionPolicy}가 담당하고,
 * 이 서비스는 권한 검사 + 입력 검증 + DTO 매핑만 책임진다(Command/Query와 Policy 분리).
 */
@Service
@RequiredArgsConstructor
public class WorkflowTransitionRuleService {

    private static final Set<TargetType> SUPPORTED_TARGET_TYPES = Set.of(TargetType.REQUIREMENT, TargetType.ISSUE);

    private final WorkflowTransitionRuleRepository ruleRepository;
    private final WorkflowTransitionPolicy workflowTransitionPolicy;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public WorkflowRuleListResponse list(Long projectId, TargetType targetType, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        requireSupportedTargetType(targetType);
        List<WorkflowTransitionRule> rules = ruleRepository.findByProjectIdAndTargetType(projectId, targetType);
        return new WorkflowRuleListResponse(
                rules.isEmpty(),
                rules.stream().map(WorkflowTransitionRuleResponse::from).toList());
    }

    @Transactional
    public WorkflowTransitionRuleResponse create(Long projectId, CreateWorkflowTransitionRuleRequest request,
                                                   UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        requireSupportedTargetType(request.targetType());
        requireValidStatusPair(request.targetType(), request.fromStatus(), request.toStatus());

        if (ruleRepository.existsByProjectIdAndTargetTypeAndFromStatusAndToStatus(
                projectId, request.targetType(), request.fromStatus(), request.toStatus())) {
            throw new ValidationException(
                    "이미 동일한 전이 규칙이 존재합니다: " + request.fromStatus() + " -> " + request.toStatus());
        }

        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        WorkflowTransitionRule rule = WorkflowTransitionRule.builder()
                .project(project)
                .targetType(request.targetType())
                .fromStatus(request.fromStatus())
                .toStatus(request.toStatus())
                .allowedRole(request.allowedRole())
                .createdBy(creator)
                .build();
        return WorkflowTransitionRuleResponse.from(ruleRepository.save(rule));
    }

    /** 해당 target_type의 규칙을 전부 삭제하면 다시 자유 전이 모드로 복귀한다(§D.3, §D.4). */
    @Transactional
    public void delete(Long projectId, Long ruleId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        WorkflowTransitionRule rule = ruleRepository.findByIdAndProjectId(ruleId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("워크플로우 규칙을 찾을 수 없습니다: " + ruleId));
        ruleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public NextStatusesResponse nextStatuses(Long projectId, TargetType targetType, String fromStatus,
                                              UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        requireSupportedTargetType(targetType);
        return workflowTransitionPolicy.nextStatuses(projectId, targetType, fromStatus);
    }

    private void requireSupportedTargetType(TargetType targetType) {
        if (!SUPPORTED_TARGET_TYPES.contains(targetType)) {
            throw new ValidationException("워크플로우 규칙은 REQUIREMENT/ISSUE target_type만 지원합니다: " + targetType);
        }
    }

    private void requireValidStatusPair(TargetType targetType, String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            throw new ValidationException("fromStatus와 toStatus는 같을 수 없습니다.");
        }
        Set<String> validValues = validStatusValues(targetType);
        if (!validValues.contains(fromStatus) || !validValues.contains(toStatus)) {
            throw new ValidationException("유효하지 않은 상태 값입니다: " + fromStatus + ", " + toStatus);
        }
    }

    private Set<String> validStatusValues(TargetType targetType) {
        return switch (targetType) {
            case REQUIREMENT -> Arrays.stream(RequirementStatus.values()).map(Enum::name).collect(Collectors.toSet());
            case ISSUE -> Arrays.stream(IssueStatus.values()).map(Enum::name).collect(Collectors.toSet());
            case TEST_CASE -> Set.of();
        };
    }
}
