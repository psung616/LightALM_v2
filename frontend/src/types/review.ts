import type { TargetType } from './common';

// ADR-008(Phase 16) / 04-api.md §4.17
export type ReviewCycleStatus = 'OPEN' | 'CLOSED';
export type ReviewDecision = 'PENDING' | 'APPROVE' | 'REJECT' | 'COMMENT_ONLY';

export interface ReviewParticipant {
  id: number;
  userId: number;
  username: string;
  fullName: string;
  decision: ReviewDecision;
  comment: string | null;
  decidedAt: string | null;
}

export interface ReviewCycle {
  id: number;
  targetType: TargetType;
  targetId: number;
  name: string;
  status: ReviewCycleStatus;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  closedAt: string | null;
  participants: ReviewParticipant[];
}
