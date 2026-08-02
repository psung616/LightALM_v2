import { apiClient } from './client';
import type { PageResponse, Priority, RequirementStatus, RequirementType } from '../types/common';
import type { Requirement, RequirementLink, TraceabilityTree } from '../types/requirement';

export interface RequirementFilters {
  status?: RequirementStatus;
  type?: RequirementType;
  priority?: Priority;
  parentId?: number;
  assignedTo?: number;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface CreateRequirementRequest {
  title: string;
  description?: string;
  type: RequirementType;
  priority?: Priority;
  parentRequirementId?: number;
  assignedTo?: number;
  dueDate?: string;
}

export type UpdateRequirementRequest = CreateRequirementRequest;

export async function listRequirements(projectId: number, filters: RequirementFilters = {}): Promise<PageResponse<Requirement>> {
  const { data } = await apiClient.get<PageResponse<Requirement>>(`/projects/${projectId}/requirements`, { params: filters });
  return data;
}

export async function getRequirement(projectId: number, reqId: number): Promise<Requirement> {
  const { data } = await apiClient.get<Requirement>(`/projects/${projectId}/requirements/${reqId}`);
  return data;
}

export async function createRequirement(projectId: number, request: CreateRequirementRequest): Promise<Requirement> {
  const { data } = await apiClient.post<Requirement>(`/projects/${projectId}/requirements`, request);
  return data;
}

export async function updateRequirement(projectId: number, reqId: number, request: UpdateRequirementRequest): Promise<Requirement> {
  const { data } = await apiClient.put<Requirement>(`/projects/${projectId}/requirements/${reqId}`, request);
  return data;
}

export async function changeRequirementStatus(projectId: number, reqId: number, status: RequirementStatus): Promise<Requirement> {
  const { data } = await apiClient.patch<Requirement>(`/projects/${projectId}/requirements/${reqId}/status`, { status });
  return data;
}

export async function deleteRequirement(projectId: number, reqId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/requirements/${reqId}`);
}

export async function listRequirementChildren(projectId: number, reqId: number): Promise<Requirement[]> {
  const { data } = await apiClient.get<Requirement[]>(`/projects/${projectId}/requirements/${reqId}/children`);
  return data;
}

export async function listRequirementLinks(projectId: number, reqId: number): Promise<RequirementLink[]> {
  const { data } = await apiClient.get<RequirementLink[]>(`/projects/${projectId}/requirements/${reqId}/links`);
  return data;
}

export async function getTraceabilityTree(projectId: number, reqId: number): Promise<TraceabilityTree> {
  const { data } = await apiClient.get<TraceabilityTree>(`/projects/${projectId}/requirements/${reqId}/traceability-tree`);
  return data;
}
