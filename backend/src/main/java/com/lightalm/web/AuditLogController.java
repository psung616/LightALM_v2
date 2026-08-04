package com.lightalm.web;

import com.lightalm.domain.AuditTargetType;
import com.lightalm.dto.AuditLogResponse;
import com.lightalm.dto.PageResponse;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.AuditLogService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> list(@PathVariable Long projectId,
                                                @RequestParam(required = false) AuditTargetType targetType,
                                                @RequestParam(required = false) Long targetId,
                                                @RequestParam(required = false) Long actorId,
                                                @RequestParam(required = false) LocalDate fromDate,
                                                @RequestParam(required = false) LocalDate toDate,
                                                @AuthenticationPrincipal UserPrincipal principal,
                                                Pageable pageable) {
        return auditLogService.list(projectId, targetType, targetId, actorId, fromDate, toDate, principal, pageable);
    }

    @GetMapping("/{targetType}/{targetId}/audit-logs")
    public List<AuditLogResponse> listForTarget(@PathVariable Long projectId, @PathVariable String targetType,
                                                 @PathVariable Long targetId, @AuthenticationPrincipal UserPrincipal principal) {
        return auditLogService.listForTarget(projectId, toAuditTargetType(targetType), targetId, principal);
    }

    private AuditTargetType toAuditTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> AuditTargetType.REQUIREMENT;
            case "issues" -> AuditTargetType.ISSUE;
            default -> throw new ValidationException("targetType은 requirements 또는 issues여야 합니다.");
        };
    }
}
