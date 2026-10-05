import { apiClient } from './client';
import type { BaselineDetail, BaselineDiff, BaselineItemRef, BaselineSummary } from '../types/baseline';

// ADR-008(Phase 16) / 04-api.md §4.18
export interface CreateBaselineRequest {
  name: string;
  description?: string;
  itemRefs: BaselineItemRef[];
}

export async function listBaselines(projectId: number): Promise<BaselineSummary[]> {
  const { data } = await apiClient.get<BaselineSummary[]>(`/projects/${projectId}/baselines`);
  return data;
}

export async function getBaseline(projectId: number, baselineId: number): Promise<BaselineDetail> {
  const { data } = await apiClient.get<BaselineDetail>(`/projects/${projectId}/baselines/${baselineId}`);
  return data;
}

export async function createBaseline(projectId: number, request: CreateBaselineRequest): Promise<BaselineDetail> {
  const { data } = await apiClient.post<BaselineDetail>(`/projects/${projectId}/baselines`, request);
  return data;
}

export async function getBaselineDiff(projectId: number, baselineId: number): Promise<BaselineDiff> {
  const { data } = await apiClient.get<BaselineDiff>(`/projects/${projectId}/baselines/${baselineId}/diff`);
  return data;
}
