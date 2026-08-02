import type { IssueStatus, IssueType, Priority } from './common';

export interface Issue {
  id: number;
  projectId: number;
  issueKey: string;
  title: string;
  description: string | null;
  type: IssueType;
  priority: Priority;
  status: IssueStatus;
  reporterId: number | null;
  reporterName: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}
