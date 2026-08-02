import { apiClient } from './client';
import type { IssueStatus, IssueType, PageResponse, Priority } from '../types/common';
import type { Issue } from '../types/issue';
import type { RequirementLink } from '../types/requirement';

export interface IssueFilters {
  status?: IssueStatus;
  type?: IssueType;
  priority?: Priority;
  assigneeId?: number;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface CreateIssueRequest {
  title: string;
  description?: string;
  type: IssueType;
  priority?: Priority;
  assigneeId?: number;
  dueDate?: string;
}

export type UpdateIssueRequest = CreateIssueRequest;

export async function listIssues(projectId: number, filters: IssueFilters = {}): Promise<PageResponse<Issue>> {
  const { data } = await apiClient.get<PageResponse<Issue>>(`/projects/${projectId}/issues`, { params: filters });
  return data;
}

export async function getIssue(projectId: number, issueId: number): Promise<Issue> {
  const { data } = await apiClient.get<Issue>(`/projects/${projectId}/issues/${issueId}`);
  return data;
}

export async function createIssue(projectId: number, request: CreateIssueRequest): Promise<Issue> {
  const { data } = await apiClient.post<Issue>(`/projects/${projectId}/issues`, request);
  return data;
}

export async function updateIssue(projectId: number, issueId: number, request: UpdateIssueRequest): Promise<Issue> {
  const { data } = await apiClient.put<Issue>(`/projects/${projectId}/issues/${issueId}`, request);
  return data;
}

export async function changeIssueStatus(projectId: number, issueId: number, status: IssueStatus): Promise<Issue> {
  const { data } = await apiClient.patch<Issue>(`/projects/${projectId}/issues/${issueId}/status`, { status });
  return data;
}

export async function deleteIssue(projectId: number, issueId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/issues/${issueId}`);
}

export async function listIssueLinks(projectId: number, issueId: number): Promise<RequirementLink[]> {
  const { data } = await apiClient.get<RequirementLink[]>(`/projects/${projectId}/issues/${issueId}/links`);
  return data;
}
