import { apiClient } from './client';
import type { ReviewCycle, ReviewDecision } from '../types/review';

// ADR-008(Phase 16) / 04-api.md §4.17. 리뷰 사이클은 참고용 의견 수집이며 대상 status를 바꾸지 않는다.
export type ReviewTargetPath = 'requirements' | 'issues';

export async function listReviewCycles(projectId: number, targetPath: ReviewTargetPath, targetId: number): Promise<ReviewCycle[]> {
  const { data } = await apiClient.get<ReviewCycle[]>(`/projects/${projectId}/${targetPath}/${targetId}/review-cycles`);
  return data;
}

export async function createReviewCycle(
  projectId: number,
  targetPath: ReviewTargetPath,
  targetId: number,
  request: { name: string; participantUserIds: number[] },
): Promise<ReviewCycle> {
  const { data } = await apiClient.post<ReviewCycle>(`/projects/${projectId}/${targetPath}/${targetId}/review-cycles`, request);
  return data;
}

export async function recordMyReviewDecision(
  projectId: number,
  cycleId: number,
  request: { decision: Exclude<ReviewDecision, 'PENDING'>; comment?: string },
): Promise<ReviewCycle> {
  const { data } = await apiClient.patch<ReviewCycle>(`/projects/${projectId}/review-cycles/${cycleId}/participants/me`, request);
  return data;
}

export async function closeReviewCycle(projectId: number, cycleId: number): Promise<ReviewCycle> {
  const { data } = await apiClient.patch<ReviewCycle>(`/projects/${projectId}/review-cycles/${cycleId}/status`, { status: 'CLOSED' });
  return data;
}
