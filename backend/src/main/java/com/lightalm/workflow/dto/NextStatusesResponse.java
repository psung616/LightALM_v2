package com.lightalm.workflow.dto;

import java.util.List;

/**
 * ADR-012 §D.4 GET {@code /workflow-rules/{targetType}/{fromStatus}} 응답. 화면의 "상태 변경"
 * 드롭다운과 §5.5 Workflow 차트가 공통으로 사용한다. {@code freeTransitionMode=true}면
 * {@code nextStatuses}는 fromStatus를 제외한 해당 target_type의 전체 상태 목록이다(자유 전이).
 */
public record NextStatusesResponse(
        boolean freeTransitionMode,
        List<String> nextStatuses
) {
}
