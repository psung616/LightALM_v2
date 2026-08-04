package com.lightalm.service;

import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditLog;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.User;
import com.lightalm.dto.AuditLogResponse;
import com.lightalm.dto.PageResponse;
import com.lightalm.repository.AuditLogRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberService projectMemberService;

    @Transactional
    public void record(Long projectId, AuditTargetType targetType, Long targetId, AuditAction action,
                        String fieldName, String oldValue, String newValue, Long actorId) {
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        AuditLog log = AuditLog.builder()
                .project(project)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .actor(actor)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional
    public void recordIfChanged(Long projectId, AuditTargetType targetType, Long targetId, Long actorId,
                                 String fieldName, Object oldValue, Object newValue) {
        recordIfChanged(projectId, targetType, targetId, actorId, fieldName, oldValue, newValue, AuditAction.UPDATE);
    }

    @Transactional
    public void recordIfChanged(Long projectId, AuditTargetType targetType, Long targetId, Long actorId,
                                 String fieldName, Object oldValue, Object newValue, AuditAction action) {
        String oldStr = oldValue != null ? String.valueOf(oldValue) : null;
        String newStr = newValue != null ? String.valueOf(newValue) : null;
        if (Objects.equals(oldStr, newStr)) {
            return;
        }
        record(projectId, targetType, targetId, action, fieldName, oldStr, newStr, actorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(Long projectId, AuditTargetType targetType, Long targetId, Long actorId,
                                                LocalDate fromDate, LocalDate toDate, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Specification<AuditLog> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (targetType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }
        if (targetId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetId"), targetId));
        }
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actor").get("id"), actorId));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
        }
        return PageResponse.from(auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForTarget(Long projectId, AuditTargetType targetType, Long targetId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return auditLogRepository.findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(projectId, targetType, targetId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
