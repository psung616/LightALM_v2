import { apiClient } from './client';
import type { AuditLog } from '../types/auditLog';

export type AuditLogTargetType = 'requirements' | 'issues';

export async function listAuditLogsForTarget(projectId: number, targetType: AuditLogTargetType, targetId: number): Promise<AuditLog[]> {
  const { data } = await apiClient.get<AuditLog[]>(`/projects/${projectId}/${targetType}/${targetId}/audit-logs`);
  return data;
}
