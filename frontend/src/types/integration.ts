import type { BuildStatus, GitLinkSource, PrStatus } from './common';

export interface GitLink {
  id: number;
  source: GitLinkSource;
  commitSha: string | null;
  prNumber: number | null;
  prStatus: PrStatus | null;
  message: string | null;
  authorLogin: string | null;
  url: string;
  linkedAt: string;
}

export interface JenkinsBuild {
  id: number;
  jobName: string;
  buildNumber: number;
  status: BuildStatus;
  buildUrl: string;
  triggeredBy: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
}
