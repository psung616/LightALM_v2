package com.lightalm.workflow.dto;

import java.util.List;

/**
 * ADR-012 §D.4 GET {@code /config/workflow-rules} 응답. {@code freeTransitionMode=true}면
 * 이 프로젝트+target_type에 등록된 규칙이 하나도 없어 모든 상태 전이가 자유롭다는 뜻이다.
 */
public record WorkflowRuleListResponse(
        boolean freeTransitionMode,
        List<WorkflowTransitionRuleResponse> rules
) {
}
