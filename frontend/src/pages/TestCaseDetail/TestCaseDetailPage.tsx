import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getTestCase, updateTestCase } from '../../api/testCase';
import type { Priority, TestCaseStatus } from '../../types/common';
import { PriorityBadge, StatusBadge } from '../../components/Badge';
import { FullScreenLoader } from '../../components/FullScreenLoader';

const STATUS_OPTIONS: TestCaseStatus[] = ['DRAFT', 'READY', 'DEPRECATED'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function TestCaseDetailPage() {
  const { projectId, tcId } = useParams<{ projectId: string; tcId: string }>();
  const id = Number(projectId);
  const tid = Number(tcId);
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);

  const testCaseQuery = useQuery({
    queryKey: ['test-case', tid],
    queryFn: () => getTestCase(id, tid),
    enabled: Number.isFinite(id) && Number.isFinite(tid),
  });

  const [form, setForm] = useState<{
    title: string;
    description: string;
    preconditions: string;
    steps: string;
    expectedResult: string;
    priority: Priority;
  }>({
    title: '',
    description: '',
    preconditions: '',
    steps: '',
    expectedResult: '',
    priority: 'MEDIUM',
  });

  function startEdit() {
    if (!testCaseQuery.data) return;
    const tc = testCaseQuery.data;
    setForm({
      title: tc.title,
      description: tc.description ?? '',
      preconditions: tc.preconditions ?? '',
      steps: tc.steps,
      expectedResult: tc.expectedResult,
      priority: tc.priority,
    });
    setEditing(true);
  }

  const updateMutation = useMutation({
    mutationFn: () =>
      updateTestCase(id, tid, {
        title: form.title,
        description: form.description || undefined,
        preconditions: form.preconditions || undefined,
        steps: form.steps,
        expectedResult: form.expectedResult,
        priority: form.priority,
        requirementId: testCaseQuery.data?.requirementId ?? undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-case', tid] });
      setEditing(false);
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: TestCaseStatus) => {
      const tc = testCaseQuery.data;
      if (!tc) throw new Error('테스트케이스를 찾을 수 없습니다.');
      return updateTestCase(id, tid, {
        title: tc.title,
        description: tc.description ?? undefined,
        preconditions: tc.preconditions ?? undefined,
        steps: tc.steps,
        expectedResult: tc.expectedResult,
        priority: tc.priority,
        requirementId: tc.requirementId ?? undefined,
        status,
      });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['test-case', tid] }),
  });

  if (testCaseQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (testCaseQuery.isError || !testCaseQuery.data) {
    return <p className="text-sm text-red-600">테스트케이스를 불러오지 못했습니다.</p>;
  }

  const tc = testCaseQuery.data;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase text-slate-400">{tc.tcKey}</p>
          <h1 className="text-xl font-semibold text-slate-900">{tc.title}</h1>
          {tc.requirementId && (
            <Link to={`/projects/${id}/requirements/${tc.requirementId}`} className="text-xs text-blue-600 hover:underline">
              {tc.requirementKey} 요구사항 보기
            </Link>
          )}
        </div>
        <div className="flex items-center gap-2">
          <PriorityBadge priority={tc.priority} />
          <StatusBadge status={tc.status} />
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">상태</h2>
          <select
            value={tc.status}
            onChange={(e) => statusMutation.mutate(e.target.value as TestCaseStatus)}
            className="rounded-md border border-slate-300 px-2 py-1 text-sm"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">기본 정보</h2>
          {!editing && (
            <button type="button" onClick={startEdit} className="text-sm text-slate-500 hover:underline">
              편집
            </button>
          )}
        </div>
        {editing ? (
          <div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">제목</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">설명</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={2}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">사전 조건</label>
              <textarea
                value={form.preconditions}
                onChange={(e) => setForm({ ...form, preconditions: e.target.value })}
                rows={2}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">절차</label>
              <textarea
                value={form.steps}
                onChange={(e) => setForm({ ...form, steps: e.target.value })}
                rows={4}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">예상 결과</label>
              <textarea
                value={form.expectedResult}
                onChange={(e) => setForm({ ...form, expectedResult: e.target.value })}
                rows={2}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">우선순위</label>
              <select
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value as Priority })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                {PRIORITY_OPTIONS.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => updateMutation.mutate()}
                disabled={updateMutation.isPending}
                className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
              >
                저장
              </button>
              <button type="button" onClick={() => setEditing(false)} className="rounded-md border border-slate-300 px-4 py-2 text-sm">
                취소
              </button>
            </div>
          </div>
        ) : (
          <dl className="grid grid-cols-1 gap-3 text-sm">
            <div>
              <dt className="text-slate-400">설명</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{tc.description || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">사전 조건</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{tc.preconditions || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">절차</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{tc.steps}</dd>
            </div>
            <div>
              <dt className="text-slate-400">예상 결과</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{tc.expectedResult}</dd>
            </div>
          </dl>
        )}
      </div>
    </div>
  );
}
