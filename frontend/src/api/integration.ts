import { apiClient } from './client';
import type { GitLinkSource } from '../types/common';
import type { GitLink, JenkinsBuild } from '../types/integration';

export type IntegrationTargetType = 'requirements' | 'issues';

export async function listGitLinks(projectId: number, targetType: IntegrationTargetType, targetId: number): Promise<GitLink[]> {
  const { data } = await apiClient.get<GitLink[]>(`/projects/${projectId}/${targetType}/${targetId}/git-links`);
  return data;
}

export async function createGitLink(
  projectId: number,
  targetType: IntegrationTargetType,
  targetId: number,
  source: GitLinkSource,
  commitSha?: string,
  prNumber?: number,
): Promise<GitLink> {
  const { data } = await apiClient.post<GitLink>(`/projects/${projectId}/${targetType}/${targetId}/git-links`, {
    source,
    commitSha,
    prNumber,
  });
  return data;
}

export async function deleteGitLink(projectId: number, linkId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/git-links/${linkId}`);
}

export async function listBuilds(projectId: number, targetType: IntegrationTargetType, targetId: number): Promise<JenkinsBuild[]> {
  const { data } = await apiClient.get<JenkinsBuild[]>(`/projects/${projectId}/${targetType}/${targetId}/builds`);
  return data;
}

export async function triggerBuild(projectId: number, targetType: 'REQUIREMENT' | 'ISSUE', targetId: number): Promise<void> {
  await apiClient.post(`/projects/${projectId}/jenkins/trigger`, { targetType, targetId });
}
