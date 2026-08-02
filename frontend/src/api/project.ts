import { apiClient } from './client';
import type { PageResponse, ProjectRole } from '../types/common';
import type { Project, ProjectMember } from '../types/project';

export interface CreateProjectRequest {
  projectKey: string;
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string;
  status?: string;
}

export interface GithubIntegrationRequest {
  repoOwner: string;
  repoName: string;
  accessToken?: string;
  webhookSecret?: string;
}

export interface JenkinsIntegrationRequest {
  baseUrl: string;
  jobName: string;
  apiUser?: string;
  apiToken?: string;
}

export async function listProjects(page = 0, size = 20): Promise<PageResponse<Project>> {
  const { data } = await apiClient.get<PageResponse<Project>>('/projects', { params: { page, size } });
  return data;
}

export async function getProject(id: number): Promise<Project> {
  const { data } = await apiClient.get<Project>(`/projects/${id}`);
  return data;
}

export async function createProject(request: CreateProjectRequest): Promise<Project> {
  const { data } = await apiClient.post<Project>('/projects', request);
  return data;
}

export async function updateProject(id: number, request: UpdateProjectRequest): Promise<Project> {
  const { data } = await apiClient.put<Project>(`/projects/${id}`, request);
  return data;
}

export async function deleteProject(id: number): Promise<void> {
  await apiClient.delete(`/projects/${id}`);
}

export async function updateGithubIntegration(id: number, request: GithubIntegrationRequest): Promise<Project> {
  const { data } = await apiClient.put<Project>(`/projects/${id}/integrations/github`, request);
  return data;
}

export async function updateJenkinsIntegration(id: number, request: JenkinsIntegrationRequest): Promise<Project> {
  const { data } = await apiClient.put<Project>(`/projects/${id}/integrations/jenkins`, request);
  return data;
}

export async function listMembers(projectId: number): Promise<ProjectMember[]> {
  const { data } = await apiClient.get<ProjectMember[]>(`/projects/${projectId}/members`);
  return data;
}

export async function addMember(projectId: number, userId: number, role: ProjectRole): Promise<ProjectMember> {
  const { data } = await apiClient.post<ProjectMember>(`/projects/${projectId}/members`, { userId, role });
  return data;
}

export async function updateMemberRole(projectId: number, userId: number, role: ProjectRole): Promise<ProjectMember> {
  const { data } = await apiClient.put<ProjectMember>(`/projects/${projectId}/members/${userId}`, { role });
  return data;
}

export async function removeMember(projectId: number, userId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/members/${userId}`);
}
