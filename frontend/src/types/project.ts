import type { ProjectRole, ProjectStatus } from './common';

export interface Project {
  id: number;
  projectKey: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  githubRepoOwner: string | null;
  githubRepoName: string | null;
  githubAccessTokenMasked: string | null;
  githubWebhookSecretMasked: string | null;
  jenkinsBaseUrl: string | null;
  jenkinsJobName: string | null;
  jenkinsApiUser: string | null;
  jenkinsApiTokenMasked: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectMember {
  id: number;
  userId: number;
  username: string;
  fullName: string;
  role: ProjectRole;
  joinedAt: string;
}
