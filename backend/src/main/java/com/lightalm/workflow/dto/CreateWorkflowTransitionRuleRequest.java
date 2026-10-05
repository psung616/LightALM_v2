package com.lightalm.workflow.dto;

import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * ADR-012 §D.4 POST 요청 바디. {@code allowedRole}을 생략하면 기존과 동일하게 MEMBER+로
 * 취급한다(§D.2). {@code targetType}은 REQUIREMENT/ISSUE만 서비스 레이어가 허용한다(§D.1).
 */
public record CreateWorkflowTransitionRuleRequest(
        @NotNull(message = "targetType은 필수입니다.") TargetType targetType,
        @NotBlank(message = "fromStatus는 필수입니다.") String fromStatus,
        @NotBlank(message = "toStatus는 필수입니다.") String toStatus,
        ProjectRole allowedRole
) {
}
