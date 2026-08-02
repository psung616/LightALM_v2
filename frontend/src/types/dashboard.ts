export interface ProjectDashboardSummary {
  requirementCountsByStatus: Record<string, number>;
  issueCountsByStatus: Record<string, number>;
  recentRequirements: import('./requirement').Requirement[];
  recentIssues: import('./issue').Issue[];
}

export interface AssignedItem {
  type: 'ISSUE' | 'REQUIREMENT';
  id: number;
  key: string;
  title: string;
  projectKey: string;
  dueDate: string;
  status: string;
}

export interface ProjectSummary {
  projectId: number;
  projectKey: string;
  projectName: string;
  assignedIssueCount: number;
  assignedRequirementCount: number;
}

export interface MyDashboard {
  assignedIssuesByStatus: Record<string, number>;
  assignedRequirementsByStatus: Record<string, number>;
  overdue: AssignedItem[];
  dueSoon: AssignedItem[];
  byProject: ProjectSummary[];
}
