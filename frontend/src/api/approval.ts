import { apiClient } from './client';
import type { ApprovalDecision, ApprovalStatus, PageResponse, RequirementStatus } from '../types/common';
import type { ApprovalRequest } from '../types/approval';

export async function createApprovalRequest(projectId: number, reqId: number, requestedStatus: RequirementStatus): Promise<ApprovalRequest> {
  const { data } = await apiClient.post<ApprovalRequest>(`/projects/${projectId}/requirements/${reqId}/approval-requests`, {
    requestedStatus,
  });
  return data;
}

export async function listApprovalRequests(projectId: number, status?: ApprovalStatus): Promise<PageResponse<ApprovalRequest>> {
  const { data } = await apiClient.get<PageResponse<ApprovalRequest>>(`/projects/${projectId}/approval-requests`, {
    params: { status, size: 100 },
  });
  return data;
}

export async function decideApproval(
  projectId: number,
  approvalId: number,
  decision: ApprovalDecision,
  comment?: string,
): Promise<ApprovalRequest> {
  const { data } = await apiClient.patch<ApprovalRequest>(`/projects/${projectId}/approval-requests/${approvalId}/decision`, {
    decision,
    comment,
  });
  return data;
}
