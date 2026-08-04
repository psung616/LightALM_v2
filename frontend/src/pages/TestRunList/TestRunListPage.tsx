import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createTestRun, listTestRuns } from '../../api/testRun';
import { listReleases } from '../../api/release';
import { StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';

export function TestRunListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [releaseId, setReleaseId] = useState('');

  const testRunsQuery = useQuery({
    queryKey: ['project', id, 'test-runs'],
    queryFn: () => listTestRuns(id, { size: 100 }),
    enabled: Number.isFinite(id),
  });

  const releasesQuery = useQuery({
    queryKey: ['project', id, 'releases', 'all'],
    queryFn: () => listReleases(id, { size: 200 }),
    enabled: Number.isFinite(id),
  });

  const createMutation = useMutation({
    mutationFn: () => createTestRun(id, name, releaseId ? Number(releaseId) : undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'test-runs'] });
      setShowCreate(false);
      setName('');
      setReleaseId('');
    },
  });

  const testRuns = testRunsQuery.data?.content ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">테스트런 목록</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
        >
          새 테스트런
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">이름</th>
              <th className="px-3 py-2">상태</th>
              <th className="px-3 py-2">진행 현황</th>
              <th className="px-3 py-2">생성일</th>
            </tr>
          </thead>
          <tbody>
            {testRuns.map((run) => {
              const total = run.results.length;
              const passed = run.results.filter((r) => r.result === 'PASS').length;
              const failed = run.results.filter((r) => r.result === 'FAIL').length;
              return (
                <tr key={run.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                  <td className="px-3 py-2">
                    <Link to={`/projects/${id}/test-runs/${run.id}`} className="font-medium text-slate-700 hover:underline">
                      {run.name}
                    </Link>
                  </td>
                  <td className="px-3 py-2"><StatusBadge status={run.status} /></td>
                  <td className="px-3 py-2 text-slate-500">
                    {total === 0 ? '항목 없음' : `${passed}/${total} 통과${failed > 0 ? `, ${failed} 실패` : ''}`}
                  </td>
                  <td className="px-3 py-2 text-slate-500">{run.createdAt?.slice(0, 10)}</td>
                </tr>
              );
            })}
            {testRuns.length === 0 && (
              <tr>
                <td colSpan={4} className="px-3 py-6 text-center text-slate-400">
                  테스트런이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <Modal title="새 테스트런" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createMutation.mutate();
            }}
          >
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">이름</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="예: Sprint 12 회귀 테스트"
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">릴리스</label>
              <select
                value={releaseId}
                onChange={(e) => setReleaseId(e.target.value)}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">없음</option>
                {releasesQuery.data?.content.map((r) => (
                  <option key={r.id} value={r.id}>{r.version}{r.name ? ` - ${r.name}` : ''}</option>
                ))}
              </select>
            </div>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="w-full rounded-md bg-primary px-3 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
            >
              생성
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}
