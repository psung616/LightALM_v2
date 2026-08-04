import type { ApprovalStatus, RequirementStatus, TargetType } from './common';

export interface ApprovalRequest {
  id: number;
  projectId: number;
  targetType: TargetType;
  targetId: number;
  targetKey: string;
  targetTitle: string;
  requestedStatus: RequirementStatus;
  requestedById: number | null;
  requestedByName: string | null;
  status: ApprovalStatus;
  approverId: number | null;
  approverName: string | null;
  comment: string | null;
  requestedAt: string;
  resolvedAt: string | null;
}
