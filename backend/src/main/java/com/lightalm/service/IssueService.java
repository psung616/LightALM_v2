package com.lightalm.service;

import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangeIssueStatusRequest;
import com.lightalm.dto.CreateIssueRequest;
import com.lightalm.dto.IssueResponse;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.UpdateIssueRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(Long projectId, IssueStatus status, IssueType type, Priority priority,
                                             Long assigneeId, String keyword, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Specification<Issue> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("title")), like));
                predicates.add(cb.like(cb.lower(root.get("issueKey")), like));
                return cb.or(predicates.toArray(new Predicate[0]));
            });
        }
        return PageResponse.from(issueRepository.findAll(spec, pageable).map(IssueResponse::from));
    }

    @Transactional(readOnly = true)
    public IssueResponse get(Long projectId, Long issueId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return IssueResponse.from(getEntity(projectId, issueId));
    }

    @Transactional
    public IssueResponse create(Long projectId, CreateIssueRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Project project = projectService.getEntity(projectId);
        User reporter = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        User assignee = resolveAssignee(request.getAssigneeId());

        String issueKey = projectService.nextIssueKey(projectId);
        Issue issue = Issue.builder()
                .project(project)
                .issueKey(issueKey)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .reporter(reporter)
                .assignee(assignee)
                .dueDate(request.getDueDate())
                .build();
        Issue saved = issueRepository.save(issue);
        auditLogService.record(projectId, AuditTargetType.ISSUE, saved.getId(), AuditAction.CREATE,
                null, null, saved.getTitle(), principal.getId());
        return IssueResponse.from(saved);
    }

    @Transactional
    public IssueResponse update(Long projectId, Long issueId, UpdateIssueRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Issue issue = getEntity(projectId, issueId);

        String oldTitle = issue.getTitle();
        String oldDescription = issue.getDescription();
        IssueType oldType = issue.getType();
        Priority oldPriority = issue.getPriority();
        Long oldAssigneeId = issue.getAssignee() != null ? issue.getAssignee().getId() : null;
        LocalDate oldDueDate = issue.getDueDate();

        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setType(request.getType());
        issue.setPriority(request.getPriority() != null ? request.getPriority() : issue.getPriority());
        issue.setAssignee(resolveAssignee(request.getAssigneeId()));
        issue.setDueDate(request.getDueDate());

        Long newAssigneeId = issue.getAssignee() != null ? issue.getAssignee().getId() : null;
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "title", oldTitle, issue.getTitle());
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "description", oldDescription, issue.getDescription());
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "type", oldType, issue.getType());
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "priority", oldPriority, issue.getPriority());
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "assigneeId", oldAssigneeId, newAssigneeId);
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "dueDate", oldDueDate, issue.getDueDate());
        return IssueResponse.from(issue);
    }

    @Transactional
    public IssueResponse changeStatus(Long projectId, Long issueId, ChangeIssueStatusRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Issue issue = getEntity(projectId, issueId);
        IssueStatus oldStatus = issue.getStatus();
        issue.setStatus(request.getStatus());
        if (request.getStatus() == IssueStatus.DONE) {
            issue.setResolvedAt(LocalDateTime.now());
        } else {
            issue.setResolvedAt(null);
        }
        auditLogService.recordIfChanged(projectId, AuditTargetType.ISSUE, issueId, principal.getId(), "status", oldStatus, issue.getStatus(), AuditAction.STATUS_CHANGE);
        return IssueResponse.from(issue);
    }

    @Transactional
    public void delete(Long projectId, Long issueId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Issue issue = getEntity(projectId, issueId);
        auditLogService.record(projectId, AuditTargetType.ISSUE, issueId, AuditAction.DELETE,
                null, issue.getTitle(), null, principal.getId());
        issueRepository.delete(issue);
    }

    private User resolveAssignee(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    Issue getEntity(Long projectId, Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + issueId));
        if (!issue.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + issueId);
        }
        return issue;
    }
}
