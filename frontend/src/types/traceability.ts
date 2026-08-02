import type { LinkType, TargetType } from './common';

export interface MatrixRequirement {
  id: number;
  reqKey: string;
  title: string;
}

export interface MatrixIssue {
  id: number;
  issueKey: string;
  title: string;
}

export interface MatrixLink {
  id: number;
  requirementId: number;
  issueId: number;
  linkType: LinkType;
}

export interface TraceabilityMatrix {
  requirements: MatrixRequirement[];
  issues: MatrixIssue[];
  links: MatrixLink[];
}

export interface CreateTraceabilityLinkRequest {
  sourceType: TargetType;
  sourceId: number;
  targetType: TargetType;
  targetId: number;
  linkType: LinkType;
}
