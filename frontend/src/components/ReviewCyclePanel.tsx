import { useState } from 'react';
import axios from 'axios';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { closeReviewCycle, createReviewCycle, listReviewCycles, recordMyReviewDecision } from '../api/review';
import type { ReviewTargetPath } from '../api/review';
import { listMembers } from '../api/project';
import { useAuth } from '../auth/AuthContext';
import type { ApiErrorResponse } from '../types/common';
import type { ReviewCycle, ReviewDecision } from '../types/review';

// ADR-008(Phase 16) / 05-frontend.md §5.11. 리뷰 사이클 위젯(요구사항/이슈 상세 화면).
// 리뷰는 "참고용 의견 수집"이며 대상 status를 절대 바꾸지 않는다(03-data-model.md §3.19).
// 상태 전이 게이트인 "승인 요청"(§5.10)과 혼동하지 않도록 라벨/안내 문구를 분리한다.

const DECISION_LABEL: Record<ReviewDecision, string> = {
  PENDING: '대기',
  APPROVE: '승인',
  REJECT: '반려',
  COMMENT_ONLY: '의견만',
};

const DECISION_COLOR: Record<ReviewDecision, string> = {
  PENDING: 'bg-amber-100 text-amber-700',
  APPROVE: 'bg-green-100 text-green-700',
  REJECT: 'bg-red-100 text-red-700',
  COMMENT_ONLY: 'bg-slate-100 text-slate-600',
};

type FinalDecision = Exclude<ReviewDecision, 'PENDING'>;

function errorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiErrorResponse>(err) && err.response?.data?.message) {
    return err.response.data.message;
  }
  return fallback;
}

function Avatar({ name }: { name: string }) {
  return (
    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-700">
      {name.slice(0, 1).toUpperCase()}
    </span>
  );
}

function DecisionForm({ projectId, cycle, queryKey }: { projectId: number; cycle: ReviewCycle; queryKey: unknown[] }) {
  const queryClient = useQueryClient();
  const [decision, setDecision] = useState<FinalDecision>('APPROVE');
  const [comment, setComment] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => recordMyReviewDecision(projectId, cycle.id, { decision, comment: comment || undefined }),
    onSuccess: () => {
      setError(null);
      setComment('');
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => setError(errorMessage(err, '결정 기록에 실패했습니다.')),
  });

  return (
    <div className="mt-3 rounded-md bg-slate-50 p-3">
      <p className="mb-2 text-xs font-medium text-slate-600">내 의견 기록</p>
      <div className="flex gap-2">
        <select
          value={decision}
          onChange={(e) => setDecision(e.target.value as FinalDecision)}
          className="rounded-md border border-slate-300 px-2 py-1 text-sm"
        >
          <option value="APPROVE">승인</option>
          <option value="REJECT">반려</option>
          <option value="COMMENT_ONLY">의견만</option>
        </select>
        <input
          type="text"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="코멘트(선택)"
          className="flex-1 rounded-md border border-slate-300 px-3 py-1 text-sm"
        />
        <button
          type="button"
          disabled={mutation.isPending}
          onClick={() => mutation.mutate()}
          className="rounded-md bg-primary px-3 py-1 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
        >
          기록
        </button>
      </div>
      {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
    </div>
  );
}

export function ReviewCyclePanel({
  projectId,
  targetPath,
  targetId,
}: {
  projectId: number;
  targetPath: ReviewTargetPath;
  targetId: number;
}) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const queryKey = ['review-cycles', projectId, targetPath, targetId];

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [participantIds, setParticipantIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);

  const cyclesQuery = useQuery({
    queryKey,
    queryFn: () => listReviewCycles(projectId, targetPath, targetId),
  });

  const membersQuery = useQuery({
    queryKey: ['project', projectId, 'members'],
    queryFn: () => listMembers(projectId),
  });

  const myRole = membersQuery.data?.find((m) => m.userId === user?.id)?.role;
  const canManage = user?.systemRole === 'ADMIN' || myRole === 'PROJECT_ADMIN' || myRole === 'MEMBER';

  const createMutation = useMutation({
    mutationFn: () => createReviewCycle(projectId, targetPath, targetId, { name, participantUserIds: participantIds }),
    onSuccess: () => {
      setError(null);
      setName('');
      setParticipantIds([]);
      setShowCreate(false);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => setError(errorMessage(err, '리뷰 사이클 생성에 실패했습니다.')),
  });

  const closeMutation = useMutation({
    mutationFn: (cycleId: number) => closeReviewCycle(projectId, cycleId),
    onSuccess: () => {
      setError(null);
      void queryClient.invalidateQueries({ queryKey });
    },
    onError: (err) => setError(errorMessage(err, '리뷰 사이클 닫기에 실패했습니다.')),
  });

  function toggleParticipant(userId: number) {
    setParticipantIds((prev) => (prev.includes(userId) ? prev.filter((x) => x !== userId) : [...prev, userId]));
  }

  return (
    <div className="mt-6 rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-1 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-slate-700">리뷰 (참고용 의견 수집)</h2>
        {canManage && (
          <button
            type="button"
            onClick={() => setShowCreate((v) => !v)}
            className="rounded-md border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
          >
            {showCreate ? '취소' : '리뷰 사이클 시작'}
          </button>
        )}
      </div>
      <p className="mb-3 text-xs text-slate-400">
        검토자 의견을 기록만 합니다. 리뷰 결과는 상태를 바꾸지 않으며, 상태 전이는 "승인 요청(상태 전이 게이트)" 또는 상태 변경을 사용하세요.
      </p>

      {error && <p className="mb-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

      {showCreate && (
        <div className="mb-4 rounded-md border border-slate-200 p-3">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="리뷰 사이클 이름 (예: 1차 설계 리뷰)"
            maxLength={150}
            className="mb-2 w-full rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          />
          <p className="mb-1 text-xs font-medium text-slate-600">참여자 (프로젝트 멤버)</p>
          <div className="mb-2 flex flex-wrap gap-3">
            {membersQuery.data?.map((m) => (
              <label key={m.userId} className="flex items-center gap-1.5 text-sm text-slate-700">
                <input type="checkbox" checked={participantIds.includes(m.userId)} onChange={() => toggleParticipant(m.userId)} />
                {m.fullName}
              </label>
            ))}
          </div>
          <button
            type="button"
            disabled={!name.trim() || participantIds.length === 0 || createMutation.isPending}
            onClick={() => createMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            생성
          </button>
        </div>
      )}

      {cyclesQuery.data?.length === 0 && <p className="text-sm text-slate-400">리뷰 사이클이 없습니다.</p>}

      <ul className="flex flex-col gap-3">
        {cyclesQuery.data?.map((cycle) => {
          const me = cycle.participants.find((p) => p.userId === user?.id);
          const isOpen = cycle.status === 'OPEN';
          return (
            <li key={cycle.id} className="rounded-md border border-slate-200 p-3">
              <div className="mb-2 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-800">{cycle.name}</p>
                  <p className="text-xs text-slate-400">
                    {cycle.createdByName ?? '알 수 없음'} · {new Date(cycle.createdAt).toLocaleString('ko-KR')}
                    {cycle.closedAt && ` · 닫힘 ${new Date(cycle.closedAt).toLocaleString('ko-KR')}`}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                      isOpen ? 'bg-blue-100 text-blue-700' : 'bg-slate-200 text-slate-600'
                    }`}
                  >
                    {isOpen ? '진행 중' : '닫힘'}
                  </span>
                  {isOpen && canManage && (
                    <button
                      type="button"
                      disabled={closeMutation.isPending}
                      onClick={() => closeMutation.mutate(cycle.id)}
                      className="rounded-md border border-slate-300 px-2 py-0.5 text-xs text-slate-700 hover:bg-slate-100 disabled:opacity-50"
                    >
                      사이클 닫기
                    </button>
                  )}
                </div>
              </div>
              <ul className="flex flex-col gap-1.5">
                {cycle.participants.map((p) => (
                  <li key={p.id} className="flex items-center gap-2 text-sm">
                    <Avatar name={p.fullName} />
                    <span className="text-slate-700">{p.fullName}</span>
                    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${DECISION_COLOR[p.decision]}`}>
                      {DECISION_LABEL[p.decision]}
                    </span>
                    {p.comment && <span className="truncate text-slate-500">— {p.comment}</span>}
                  </li>
                ))}
              </ul>
              {me && isOpen && <DecisionForm projectId={projectId} cycle={cycle} queryKey={queryKey} />}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
