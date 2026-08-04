const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-slate-100 text-slate-600',
  APPROVED: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  IMPLEMENTED: 'bg-teal-100 text-teal-700',
  VERIFIED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  TODO: 'bg-slate-100 text-slate-600',
  IN_REVIEW: 'bg-purple-100 text-purple-700',
  DONE: 'bg-green-100 text-green-700',
  CLOSED: 'bg-slate-200 text-slate-600',
  ACTIVE: 'bg-green-100 text-green-700',
  ARCHIVED: 'bg-slate-200 text-slate-600',
  READY: 'bg-blue-100 text-blue-700',
  DEPRECATED: 'bg-slate-200 text-slate-500',
  PLANNED: 'bg-slate-100 text-slate-600',
  COMPLETED: 'bg-green-100 text-green-700',
  PASS: 'bg-green-100 text-green-700',
  FAIL: 'bg-red-100 text-red-700',
  BLOCKED: 'bg-orange-100 text-orange-700',
  SKIPPED: 'bg-slate-100 text-slate-500',
  NOT_RUN: 'bg-slate-100 text-slate-400',
  RELEASED: 'bg-green-100 text-green-700',
  PENDING: 'bg-amber-100 text-amber-700',
  CANCELLED: 'bg-slate-200 text-slate-500',
};

const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  CRITICAL: 'bg-red-100 text-red-700',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[status] ?? 'bg-slate-100 text-slate-600'}`}>
      {status}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: string }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${PRIORITY_COLORS[priority] ?? 'bg-slate-100 text-slate-600'}`}>
      {priority}
    </span>
  );
}
