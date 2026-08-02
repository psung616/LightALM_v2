import type { Priority, RequirementStatus, RequirementType } from './common';

export interface Requirement {
  id: number;
  projectId: number;
  reqKey: string;
  title: string;
  description: string | null;
  type: RequirementType;
  priority: Priority;
  status: RequirementStatus;
  parentRequirementId: number | null;
  parentRequirementKey: string | null;
  createdById: number | null;
  createdByName: string | null;
  assignedToId: number | null;
  assignedToName: string | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RequirementLink {
  linkId: number;
  linkedType: 'REQUIREMENT' | 'ISSUE';
  linkedId: number;
  linkedKey: string;
  linkedTitle: string;
  linkedStatus: string;
  linkType: string;
}

export interface TraceabilityTreeAncestor {
  id: number;
  reqKey: string;
  title: string;
}

export interface TraceabilityTreeSelf {
  id: number;
  reqKey: string;
  title: string;
  status: string;
}

export interface TraceabilityTreeLinkedIssue {
  id: number;
  issueKey: string;
  title: string;
  linkType: string;
  status: string;
}

export interface TraceabilityTreeDescendant {
  id: number;
  reqKey: string;
  title: string;
  status: string;
  linkedIssues: TraceabilityTreeLinkedIssue[];
  children: TraceabilityTreeDescendant[];
}

export interface TraceabilityTree {
  ancestors: TraceabilityTreeAncestor[];
  self: TraceabilityTreeSelf;
  descendants: TraceabilityTreeDescendant[];
}
