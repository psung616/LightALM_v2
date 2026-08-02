import { apiClient } from './client';
import type { CreateTraceabilityLinkRequest, TraceabilityMatrix } from '../types/traceability';

export async function getMatrix(projectId: number): Promise<TraceabilityMatrix> {
  const { data } = await apiClient.get<TraceabilityMatrix>(`/projects/${projectId}/traceability/matrix`);
  return data;
}

export async function createLink(projectId: number, request: CreateTraceabilityLinkRequest) {
  const { data } = await apiClient.post(`/projects/${projectId}/traceability/links`, request);
  return data;
}

export async function deleteLink(projectId: number, linkId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/traceability/links/${linkId}`);
}
