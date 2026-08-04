import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { decideApproval, listApprovalRequests } from '../../api/approval';
import type { ApprovalDecision, ApprovalStatus } from '../../types/common';
import { StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';

const STATUS_OPTIONS: ApprovalStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];

export function ApprovalInboxPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [status, setStatus] = useState<ApprovalStatus | ''>('PENDING');
  const [decisionTarget, setDecisionTarget] = useState<{ approvalId: number; decision: ApprovalDecision } | null>(null);
  const [comment, setComment] = useState('');

  const approvalsQuery = useQuery({
    queryKey: ['project', id, 'approval-requests', status],
    queryFn: () => listApprovalRequests(id, (status || undefined) as ApprovalStatus | undefined),
    enabled: Number.isFinite(id),
  });

  const decideMutation = useMutation({
    mutationFn: () => decideApproval(id, decisionTarget!.approvalId, decisionTarget!.decision, comment || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'approval-requests'] });
      setDecisionTarget(null);
      setComment('');
    },
  });

  const approvals = approvalsQuery.data?.content ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">승인함</h1>
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as ApprovalStatus | '')}
          className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
        >
          <option value="">전체 상태</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">대상</th>
              <th className="px-3 py-2">요청자</th>
              <th className="px-3 py-2">요청일시</th>
              <th className="px-3 py-2">요청 상태</th>
              <th className="px-3 py-2">처리 상태</th>
              <th className="px-3 py-2">작업</th>
            </tr>
          </thead>
          <tbody>
            {approvals.map((a) => (
              <tr key={a.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-3 py-2">
                  <Link to={`/projects/${id}/requirements/${a.targetId}`} className="font-medium text-slate-700 hover:underline">
                    {a.targetKey}
                  </Link>
                  <span className="ml-2 text-slate-500">{a.targetTitle}</span>
                </td>
                <td className="px-3 py-2">{a.requestedByName ?? '-'}</td>
                <td className="px-3 py-2 text-slate-500">{new Date(a.requestedAt).toLocaleString('ko-KR')}</td>
                <td className="px-3 py-2">{a.requestedStatus}</td>
                <td className="px-3 py-2"><StatusBadge status={a.status} /></td>
                <td className="px-3 py-2">
                  {a.status === 'PENDING' && (
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => setDecisionTarget({ approvalId: a.id, decision: 'APPROVE' })}
                        className="rounded-md bg-slate-900 px-3 py-1 text-xs font-medium text-white hover:bg-slate-800"
                      >
                        승인
                      </button>
                      <button
                        type="button"
                        onClick={() => setDecisionTarget({ approvalId: a.id, decision: 'REJECT' })}
                        className="rounded-md border border-red-300 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                      >
                        반려
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {approvals.length === 0 && (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-center text-slate-400">
                  승인 요청이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {decisionTarget && (
        <Modal
          title={decisionTarget.decision === 'APPROVE' ? '승인' : '반려'}
          onClose={() => {
            setDecisionTarget(null);
            setComment('');
          }}
        >
          <div className="mb-4">
            <label className="mb-1 block text-sm text-slate-600">사유(선택)</label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <button
            type="button"
            disabled={decideMutation.isPending}
            onClick={() => decideMutation.mutate()}
            className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            {decisionTarget.decision === 'APPROVE' ? '승인 확정' : '반려 확정'}
          </button>
        </Modal>
      )}
    </div>
  );
}
