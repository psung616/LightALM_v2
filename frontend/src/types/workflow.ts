// ADR-012 §D(Phase 23) 워크플로우(상태 전이) 규칙. 04-api.md §4.27 참고.
// 대상은 REQUIREMENT/ISSUE 두 target_type만(TEST_CASE 제외) — ../types/common.ts의 TargetType을 그대로 재사용한다.

import type { ProjectRole, TargetType } from './common';

export interface WorkflowTransitionRule {
  id: number;
  targetType: TargetType;
  fromStatus: string;
  toStatus: string;
  allowedRole: ProjectRole | null;
  createdAt: string;
}

/** GET .../config/workflow-rules 응답. freeTransitionMode=true면 등록된 규칙이 하나도 없다는 뜻이다(자유 전이). */
export interface WorkflowRuleListResponse {
  freeTransitionMode: boolean;
  rules: WorkflowTransitionRule[];
}

export interface CreateWorkflowTransitionRuleRequest {
  targetType: TargetType;
  fromStatus: string;
  toStatus: string;
  allowedRole?: ProjectRole | null;
}

/** GET .../workflow-rules/{targetType}/{fromStatus} 응답 — "상태 변경" 드롭다운/Workflow 차트 구성용. */
export interface NextStatusesResponse {
  freeTransitionMode: boolean;
  nextStatuses: string[];
}
