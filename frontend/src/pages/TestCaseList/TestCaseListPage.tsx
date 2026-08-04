import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createTestCase, listTestCases } from '../../api/testCase';
import { listRequirements } from '../../api/requirement';
import type { Priority, TestCaseStatus } from '../../types/common';
import { PriorityBadge, StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';

const STATUS_OPTIONS: TestCaseStatus[] = ['DRAFT', 'READY', 'DEPRECATED'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function TestCaseListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [keyword, setKeyword] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const testCasesQuery = useQuery({
    queryKey: ['project', id, 'test-cases', { status, priority, keyword }],
    queryFn: () =>
      listTestCases(id, {
        status: (status || undefined) as TestCaseStatus | undefined,
        priority: (priority || undefined) as Priority | undefined,
        keyword: keyword || undefined,
        size: 100,
      }),
    enabled: Number.isFinite(id),
  });

  const requirementsQuery = useQuery({
    queryKey: ['project', id, 'requirements', 'all'],
    queryFn: () => listRequirements(id, { size: 200 }),
    enabled: Number.isFinite(id),
  });

  const [form, setForm] = useState({
    title: '',
    description: '',
    preconditions: '',
    steps: '',
    expectedResult: '',
    priority: 'MEDIUM' as Priority,
    requirementId: '',
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createTestCase(id, {
        title: form.title,
        description: form.description || undefined,
        preconditions: form.preconditions || undefined,
        steps: form.steps,
        expectedResult: form.expectedResult,
        priority: form.priority,
        requirementId: form.requirementId ? Number(form.requirementId) : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'test-cases'] });
      setShowCreate(false);
      setForm({ title: '', description: '', preconditions: '', steps: '', expectedResult: '', priority: 'MEDIUM', requirementId: '' });
    },
  });

  const testCases = testCasesQuery.data?.content ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">테스트케이스 목록</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          새 테스트케이스
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
          <option value="">전체 상태</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <select value={priority} onChange={(e) => setPriority(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
          <option value="">전체 우선순위</option>
          {PRIORITY_OPTIONS.map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="키워드 검색"
          className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
        />
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">키</th>
              <th className="px-3 py-2">제목</th>
              <th className="px-3 py-2">우선순위</th>
              <th className="px-3 py-2">상태</th>
              <th className="px-3 py-2">연결된 요구사항</th>
            </tr>
          </thead>
          <tbody>
            {testCases.map((tc) => (
              <tr key={tc.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-3 py-2">
                  <Link to={`/projects/${id}/test-cases/${tc.id}`} className="font-medium text-slate-700 hover:underline">
                    {tc.tcKey}
                  </Link>
                </td>
                <td className="px-3 py-2">{tc.title}</td>
                <td className="px-3 py-2"><PriorityBadge priority={tc.priority} /></td>
                <td className="px-3 py-2"><StatusBadge status={tc.status} /></td>
                <td className="px-3 py-2">{tc.requirementKey ?? '-'}</td>
              </tr>
            ))}
            {testCases.length === 0 && (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-center text-slate-400">
                  테스트케이스가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <Modal title="새 테스트케이스" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createMutation.mutate();
            }}
          >
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">제목</label>
              <input
                type="text"
                required
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
                required
                value={form.steps}
                onChange={(e) => setForm({ ...form, steps: e.target.value })}
                rows={3}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">예상 결과</label>
              <textarea
                required
                value={form.expectedResult}
                onChange={(e) => setForm({ ...form, expectedResult: e.target.value })}
                rows={2}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-4 grid grid-cols-2 gap-3">
              <div>
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
              <div>
                <label className="mb-1 block text-sm text-slate-600">연결할 요구사항</label>
                <select
                  value={form.requirementId}
                  onChange={(e) => setForm({ ...form, requirementId: e.target.value })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">없음</option>
                  {requirementsQuery.data?.content.map((r) => (
                    <option key={r.id} value={r.id}>{r.reqKey} - {r.title}</option>
                  ))}
                </select>
              </div>
            </div>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
            >
              생성
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}
