package com.lightalm.service;

import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangeRequirementStatusRequest;
import com.lightalm.dto.CreateRequirementRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RequirementResponse;
import com.lightalm.dto.UpdateRequirementRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<RequirementResponse> list(Long projectId, RequirementStatus status, RequirementType type,
                                                    Priority priority, Long parentId, Long assignedTo, String keyword,
                                                    UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Specification<Requirement> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (parentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("parentRequirement").get("id"), parentId));
        }
        if (assignedTo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assignedTo));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("title")), like));
                predicates.add(cb.like(cb.lower(root.get("reqKey")), like));
                return cb.or(predicates.toArray(new Predicate[0]));
            });
        }
        return PageResponse.from(requirementRepository.findAll(spec, pageable).map(RequirementResponse::from));
    }

    @Transactional(readOnly = true)
    public RequirementResponse get(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return RequirementResponse.from(getEntity(projectId, reqId));
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> children(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        getEntity(projectId, reqId);
        return requirementRepository.findByParentRequirementId(reqId).stream()
                .map(RequirementResponse::from)
                .toList();
    }

    @Transactional
    public RequirementResponse create(Long projectId, CreateRequirementRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        Requirement parent = null;
        if (request.getParentRequirementId() != null) {
            parent = getEntity(projectId, request.getParentRequirementId());
        }
        User assignee = resolveAssignee(request.getAssignedTo());

        String reqKey = projectService.nextRequirementKey(projectId);
        Requirement requirement = Requirement.builder()
                .project(project)
                .reqKey(reqKey)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .parentRequirement(parent)
                .createdBy(creator)
                .assignedTo(assignee)
                .dueDate(request.getDueDate())
                .build();
        Requirement saved = requirementRepository.save(requirement);
        auditLogService.record(projectId, AuditTargetType.REQUIREMENT, saved.getId(), AuditAction.CREATE,
                null, null, saved.getTitle(), principal.getId());
        return RequirementResponse.from(saved);
    }

    @Transactional
    public RequirementResponse update(Long projectId, Long reqId, UpdateRequirementRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Requirement requirement = getEntity(projectId, reqId);

        Requirement parent = null;
        if (request.getParentRequirementId() != null) {
            if (request.getParentRequirementId().equals(reqId)) {
                throw new ValidationException("요구사항은 자기 자신을 상위 요구사항으로 가질 수 없습니다.");
            }
            parent = getEntity(projectId, request.getParentRequirementId());
        }

        String oldTitle = requirement.getTitle();
        String oldDescription = requirement.getDescription();
        RequirementType oldType = requirement.getType();
        Priority oldPriority = requirement.getPriority();
        Long oldParentId = requirement.getParentRequirement() != null ? requirement.getParentRequirement().getId() : null;
        Long oldAssignedToId = requirement.getAssignedTo() != null ? requirement.getAssignedTo().getId() : null;
        LocalDate oldDueDate = requirement.getDueDate();

        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setType(request.getType());
        requirement.setPriority(request.getPriority() != null ? request.getPriority() : requirement.getPriority());
        requirement.setParentRequirement(parent);
        requirement.setAssignedTo(resolveAssignee(request.getAssignedTo()));
        requirement.setDueDate(request.getDueDate());

        Long newParentId = requirement.getParentRequirement() != null ? requirement.getParentRequirement().getId() : null;
        Long newAssignedToId = requirement.getAssignedTo() != null ? requirement.getAssignedTo().getId() : null;
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "title", oldTitle, requirement.getTitle());
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "description", oldDescription, requirement.getDescription());
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "type", oldType, requirement.getType());
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "priority", oldPriority, requirement.getPriority());
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "parentRequirementId", oldParentId, newParentId);
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "assignedTo", oldAssignedToId, newAssignedToId);
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "dueDate", oldDueDate, requirement.getDueDate());
        return RequirementResponse.from(requirement);
    }

    @Transactional
    public RequirementResponse changeStatus(Long projectId, Long reqId, ChangeRequirementStatusRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Requirement requirement = getEntity(projectId, reqId);
        RequirementStatus oldStatus = requirement.getStatus();
        if (oldStatus == RequirementStatus.DRAFT && request.getStatus() == RequirementStatus.APPROVED) {
            throw new ValidationException("DRAFT → APPROVED 전이는 승인 요청을 통해서만 가능합니다. POST .../approval-requests를 사용하세요.");
        }
        requirement.setStatus(request.getStatus());
        auditLogService.recordIfChanged(projectId, AuditTargetType.REQUIREMENT, reqId, principal.getId(), "status", oldStatus, requirement.getStatus(), AuditAction.STATUS_CHANGE);
        return RequirementResponse.from(requirement);
    }

    @Transactional
    public void delete(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Requirement requirement = getEntity(projectId, reqId);
        auditLogService.record(projectId, AuditTargetType.REQUIREMENT, reqId, AuditAction.DELETE,
                null, requirement.getTitle(), null, principal.getId());
        requirementRepository.delete(requirement);
    }

    private User resolveAssignee(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Requirement getEntity(Long projectId, Long reqId) {
        Requirement requirement = requirementRepository.findById(reqId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId));
        if (!requirement.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId);
        }
        return requirement;
    }
}
