import { apiClient } from './client';
import type { TargetType } from '../types/common';
import type {
  CreateWorkflowTransitionRuleRequest,
  NextStatusesResponse,
  WorkflowRuleListResponse,
  WorkflowTransitionRule,
} from '../types/workflow';

export async function listWorkflowTransitionRules(
  projectId: number,
  targetType: TargetType,
): Promise<WorkflowRuleListResponse> {
  const { data } = await apiClient.get<WorkflowRuleListResponse>(
    `/projects/${projectId}/config/workflow-rules`,
    { params: { targetType } },
  );
  return data;
}

export async function createWorkflowTransitionRule(
  projectId: number,
  request: CreateWorkflowTransitionRuleRequest,
): Promise<WorkflowTransitionRule> {
  const { data } = await apiClient.post<WorkflowTransitionRule>(
    `/projects/${projectId}/config/workflow-rules`,
    request,
  );
  return data;
}

export async function deleteWorkflowTransitionRule(projectId: number, ruleId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/config/workflow-rules/${ruleId}`);
}

export async function getWorkflowNextStatuses(
  projectId: number,
  targetType: TargetType,
  fromStatus: string,
): Promise<NextStatusesResponse> {
  const { data } = await apiClient.get<NextStatusesResponse>(
    `/projects/${projectId}/workflow-rules/${targetType}/${fromStatus}`,
  );
  return data;
}
