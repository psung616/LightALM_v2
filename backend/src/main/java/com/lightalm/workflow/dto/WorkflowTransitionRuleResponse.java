package com.lightalm.workflow.dto;

import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import java.time.LocalDateTime;

public record WorkflowTransitionRuleResponse(
        Long id,
        TargetType targetType,
        String fromStatus,
        String toStatus,
        ProjectRole allowedRole,
        LocalDateTime createdAt
) {

    public static WorkflowTransitionRuleResponse from(WorkflowTransitionRule rule) {
        return new WorkflowTransitionRuleResponse(
                rule.getId(),
                rule.getTargetType(),
                rule.getFromStatus(),
                rule.getToStatus(),
                rule.getAllowedRole(),
                rule.getCreatedAt());
    }
}
