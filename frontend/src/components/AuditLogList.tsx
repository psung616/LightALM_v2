import type { AuditLog } from '../types/auditLog';

function describe(log: AuditLog): string {
  const actor = log.actorName ?? '알 수 없는 사용자';
  switch (log.action) {
    case 'CREATE':
      return `${actor}님이 생성했습니다`;
    case 'UPDATE':
      return `${actor}님이 ${log.fieldName ?? '항목'}을(를) 변경했습니다: ${log.oldValue ?? '(없음)'} → ${log.newValue ?? '(없음)'}`;
    case 'STATUS_CHANGE':
      return `${actor}님이 상태를 변경했습니다: ${log.oldValue ?? '(없음)'} → ${log.newValue ?? '(없음)'}`;
    case 'DELETE':
      return `${actor}님이 삭제했습니다`;
    case 'APPROVE':
      return `${actor}님이 승인했습니다`;
    case 'REJECT':
      return `${actor}님이 반려했습니다`;
    default:
      return `${actor}님이 변경했습니다`;
  }
}

export function AuditLogList({ logs }: { logs: AuditLog[] }) {
  if (logs.length === 0) {
    return <p className="text-sm text-slate-400">변경 이력이 없습니다.</p>;
  }
  return (
    <ul className="flex flex-col gap-1.5">
      {logs.map((log) => (
        <li key={log.id} className="flex items-center justify-between text-sm">
          <span className="text-slate-600">{describe(log)}</span>
          <span className="ml-3 shrink-0 text-xs text-slate-400">{new Date(log.createdAt).toLocaleString('ko-KR')}</span>
        </li>
      ))}
    </ul>
  );
}
