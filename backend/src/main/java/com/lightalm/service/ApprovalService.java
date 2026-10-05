package com.lightalm.service;

import com.lightalm.domain.ApprovalDecision;
import com.lightalm.domain.ApprovalRequest;
import com.lightalm.domain.ApprovalStatus;
import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.ApprovalDecisionRequest;
import com.lightalm.dto.ApprovalRequestResponse;
import com.lightalm.dto.CreateApprovalRequestRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.ApprovalRequestRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.workflow.service.WorkflowTransitionPolicy;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final AuditLogService auditLogService;
    private final WorkflowTransitionPolicy workflowTransitionPolicy;

    @Transactional
    public ApprovalRequestResponse create(Long projectId, Long reqId, CreateApprovalRequestRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Requirement requirement = getRequirementEntity(projectId, reqId);

        if (request.getRequestedStatus() != RequirementStatus.APPROVED) {
            throw new ValidationException("현재는 APPROVED 승인 요청만 지원합니다.");
        }
        if (requirement.getStatus() != RequirementStatus.DRAFT) {
            throw new ValidationException("DRAFT 상태의 요구사항만 승인 요청할 수 있습니다.");
        }
        if (approvalRequestRepository.existsByTargetTypeAndTargetIdAndStatus(TargetType.REQUIREMENT, reqId, ApprovalStatus.PENDING)) {
            throw new ValidationException("이미 대기 중인 승인 요청이 있습니다.");
        }

        User requester = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        Project project = projectService.getEntity(projectId);

        ApprovalRequest approval = ApprovalRequest.builder()
                .project(project)
                .targetType(TargetType.REQUIREMENT)
                .targetId(reqId)
                .requestedStatus(request.getRequestedStatus())
                .requestedBy(requester)
                .build();
        ApprovalRequest saved = approvalRequestRepository.save(approval);
        return ApprovalRequestResponse.from(saved, requirement.getReqKey(), requirement.getTitle());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalRequestResponse> list(Long projectId, ApprovalStatus status, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Specification<ApprovalRequest> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResponse.from(approvalRequestRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional
    public ApprovalRequestResponse decide(Long projectId, Long approvalId, ApprovalDecisionRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        ApprovalRequest approval = getEntity(projectId, approvalId);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new ValidationException("이미 처리된 승인 요청입니다.");
        }
        Requirement requirement = getRequirementEntity(projectId, approval.getTargetId());
        User approver = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        approval.setApprover(approver);
        approval.setComment(request.getComment());
        approval.setResolvedAt(LocalDateTime.now());

        if (request.getDecision() == ApprovalDecision.APPROVE) {
            RequirementStatus oldStatus = requirement.getStatus();
            // ADR-012 §D.3 AND 조건: 승인 요청이 APPROVED로 결정되는 이 지점에서 워크플로우
            // 화이트리스트도 함께 통과해야 한다. 둘 중 하나라도 막으면 전체 decide() 호출이
            // 롤백되어(요구사항 상태/승인 상태 둘 다 변경되지 않음) 전이가 거부된다.
            //
            // requireAllowedTransition(principal 포함 버전)이 아니라 requireRegisteredTransition을
            // 쓴다 — 이 메서드는 PROJECT_ADMIN 이상만 호출 가능한데, requireAllowedTransition의
            // "시스템 ADMIN/PROJECT_ADMIN은 화이트리스트 무관 항상 통과" lock-out 방지 우회를
            // 그대로 쓰면 모든 실제 호출자가 우회에 걸려 체크가 전혀 작동하지 않는다
            // (qa-tester 2026-10-05 실제 Postgres+API 검증에서 발견).
            workflowTransitionPolicy.requireRegisteredTransition(
                    projectId, TargetType.REQUIREMENT, oldStatus.name(), approval.getRequestedStatus().name());
            requirement.setStatus(approval.getRequestedStatus());
            approval.setStatus(ApprovalStatus.APPROVED);
            auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, requirement.getId(), principal.getId(),
                    "status", oldStatus, requirement.getStatus(), AuditAction.STATUS_CHANGE);
            auditLogService.record(projectId, AuditTargetType.REQUIREMENT, requirement.getId(), AuditAction.APPROVE,
                    null, null, request.getComment(), principal.getId());
        } else {
            approval.setStatus(ApprovalStatus.REJECTED);
            auditLogService.record(projectId, AuditTargetType.REQUIREMENT, requirement.getId(), AuditAction.REJECT,
                    null, null, request.getComment(), principal.getId());
        }

        return ApprovalRequestResponse.from(approval, requirement.getReqKey(), requirement.getTitle());
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest approval) {
        String targetKey = "(삭제됨)";
        String targetTitle = "(삭제됨)";
        if (approval.getTargetType() == TargetType.REQUIREMENT) {
            Requirement requirement = requirementRepository.findById(approval.getTargetId()).orElse(null);
            if (requirement != null) {
                targetKey = requirement.getReqKey();
                targetTitle = requirement.getTitle();
            }
        }
        return ApprovalRequestResponse.from(approval, targetKey, targetTitle);
    }

    private Requirement getRequirementEntity(Long projectId, Long reqId) {
        Requirement requirement = requirementRepository.findById(reqId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId));
        if (!requirement.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId);
        }
        return requirement;
    }

    private ApprovalRequest getEntity(Long projectId, Long approvalId) {
        ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("승인 요청을 찾을 수 없습니다: " + approvalId));
        if (!approval.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("승인 요청을 찾을 수 없습니다: " + approvalId);
        }
        return approval;
    }
}
