import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createRelease, listReleases } from '../../api/release';
import { StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';

export function ReleaseListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ version: '', name: '', releaseDate: '', description: '' });

  const releasesQuery = useQuery({
    queryKey: ['project', id, 'releases'],
    queryFn: () => listReleases(id, { size: 100 }),
    enabled: Number.isFinite(id),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createRelease(id, {
        version: form.version,
        name: form.name || undefined,
        releaseDate: form.releaseDate || undefined,
        description: form.description || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'releases'] });
      setShowCreate(false);
      setForm({ version: '', name: '', releaseDate: '', description: '' });
    },
  });

  const releases = releasesQuery.data?.content ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">릴리스 목록</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
        >
          새 릴리스
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">버전</th>
              <th className="px-3 py-2">이름</th>
              <th className="px-3 py-2">상태</th>
              <th className="px-3 py-2">릴리스 예정일</th>
              <th className="px-3 py-2">포함 항목</th>
            </tr>
          </thead>
          <tbody>
            {releases.map((r) => (
              <tr key={r.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-3 py-2">
                  <Link to={`/projects/${id}/releases/${r.id}`} className="font-medium text-slate-700 hover:underline">
                    {r.version}
                  </Link>
                </td>
                <td className="px-3 py-2">{r.name ?? '-'}</td>
                <td className="px-3 py-2"><StatusBadge status={r.status} /></td>
                <td className="px-3 py-2 text-slate-500">{r.releaseDate ?? '-'}</td>
                <td className="px-3 py-2 text-slate-500">{r.items.length}건</td>
              </tr>
            ))}
            {releases.length === 0 && (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-center text-slate-400">
                  릴리스가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <Modal title="새 릴리스" onClose={() => setShowCreate(false)}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createMutation.mutate();
            }}
          >
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">버전</label>
              <input
                type="text"
                required
                value={form.version}
                onChange={(e) => setForm({ ...form, version: e.target.value })}
                placeholder="예: 1.2.0"
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">이름</label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">릴리스 예정일</label>
              <input
                type="date"
                value={form.releaseDate}
                onChange={(e) => setForm({ ...form, releaseDate: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">설명</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={3}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
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
