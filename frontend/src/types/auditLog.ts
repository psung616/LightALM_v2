export type AuditTargetType = 'REQUIREMENT' | 'ISSUE' | 'TEST_CASE' | 'RELEASE' | 'PROJECT' | 'USER' | 'TRACEABILITY_LINK';
export type AuditAction = 'CREATE' | 'UPDATE' | 'STATUS_CHANGE' | 'DELETE' | 'APPROVE' | 'REJECT';

export interface AuditLog {
  id: number;
  projectId: number | null;
  targetType: AuditTargetType;
  targetId: number;
  action: AuditAction;
  fieldName: string | null;
  oldValue: string | null;
  newValue: string | null;
  actorId: number | null;
  actorName: string | null;
  createdAt: string;
}
