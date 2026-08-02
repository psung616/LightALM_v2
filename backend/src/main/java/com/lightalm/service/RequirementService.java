package com.lightalm.service;

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
        return RequirementResponse.from(requirementRepository.save(requirement));
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

        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setType(request.getType());
        requirement.setPriority(request.getPriority() != null ? request.getPriority() : requirement.getPriority());
        requirement.setParentRequirement(parent);
        requirement.setAssignedTo(resolveAssignee(request.getAssignedTo()));
        requirement.setDueDate(request.getDueDate());
        return RequirementResponse.from(requirement);
    }

    @Transactional
    public RequirementResponse changeStatus(Long projectId, Long reqId, ChangeRequirementStatusRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Requirement requirement = getEntity(projectId, reqId);
        requirement.setStatus(request.getStatus());
        return RequirementResponse.from(requirement);
    }

    @Transactional
    public void delete(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Requirement requirement = getEntity(projectId, reqId);
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
