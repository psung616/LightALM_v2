import { apiClient } from './client';
import type { PageResponse, ReleaseStatus, TargetType } from '../types/common';
import type { Release } from '../types/release';

export interface ReleaseFilters {
  page?: number;
  size?: number;
}

export interface CreateReleaseRequest {
  version: string;
  name?: string;
  releaseDate?: string;
  description?: string;
}

export type UpdateReleaseRequest = CreateReleaseRequest;

export interface ReleaseNotes {
  releaseId: number;
  version: string;
  notes: string;
}

export async function listReleases(projectId: number, filters: ReleaseFilters = {}): Promise<PageResponse<Release>> {
  const { data } = await apiClient.get<PageResponse<Release>>(`/projects/${projectId}/releases`, { params: filters });
  return data;
}

export async function getRelease(projectId: number, releaseId: number): Promise<Release> {
  const { data } = await apiClient.get<Release>(`/projects/${projectId}/releases/${releaseId}`);
  return data;
}

export async function createRelease(projectId: number, request: CreateReleaseRequest): Promise<Release> {
  const { data } = await apiClient.post<Release>(`/projects/${projectId}/releases`, request);
  return data;
}

export async function updateRelease(projectId: number, releaseId: number, request: UpdateReleaseRequest): Promise<Release> {
  const { data } = await apiClient.put<Release>(`/projects/${projectId}/releases/${releaseId}`, request);
  return data;
}

export async function changeReleaseStatus(projectId: number, releaseId: number, status: ReleaseStatus): Promise<Release> {
  const { data } = await apiClient.patch<Release>(`/projects/${projectId}/releases/${releaseId}/status`, { status });
  return data;
}

export async function addReleaseItem(projectId: number, releaseId: number, targetType: TargetType, targetId: number): Promise<Release> {
  const { data } = await apiClient.post<Release>(`/projects/${projectId}/releases/${releaseId}/items`, { targetType, targetId });
  return data;
}

export async function removeReleaseItem(projectId: number, releaseId: number, itemId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/releases/${releaseId}/items/${itemId}`);
}

export async function getReleaseNotes(projectId: number, releaseId: number): Promise<ReleaseNotes> {
  const { data } = await apiClient.get<ReleaseNotes>(`/projects/${projectId}/releases/${releaseId}/notes`);
  return data;
}
