export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export type SystemRole = 'ADMIN' | 'USER';
export type ProjectRole = 'PROJECT_ADMIN' | 'MEMBER' | 'VIEWER';
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export type RequirementType = 'FUNCTIONAL' | 'NON_FUNCTIONAL' | 'BUSINESS';
export type RequirementStatus = 'DRAFT' | 'APPROVED' | 'IN_PROGRESS' | 'IMPLEMENTED' | 'VERIFIED' | 'REJECTED';

export type IssueType = 'BUG' | 'TASK' | 'STORY' | 'IMPROVEMENT';
export type IssueStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CLOSED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type TargetType = 'REQUIREMENT' | 'ISSUE';
export type LinkType = 'IMPLEMENTS' | 'TESTS' | 'DEPENDS_ON' | 'RELATES_TO' | 'DUPLICATES';

export type GitLinkSource = 'COMMIT' | 'PULL_REQUEST';
export type PrStatus = 'OPEN' | 'MERGED' | 'CLOSED';
export type BuildStatus = 'SUCCESS' | 'FAILURE' | 'UNSTABLE' | 'RUNNING' | 'ABORTED';

export type TestCaseStatus = 'DRAFT' | 'READY' | 'DEPRECATED';
export type TestRunStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';
export type TestResult = 'NOT_RUN' | 'PASS' | 'FAIL' | 'BLOCKED' | 'SKIPPED';

export type ReleaseStatus = 'PLANNED' | 'IN_PROGRESS' | 'RELEASED' | 'ARCHIVED';

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type ApprovalDecision = 'APPROVE' | 'REJECT';
