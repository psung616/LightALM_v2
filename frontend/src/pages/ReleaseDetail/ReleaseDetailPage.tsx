import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addReleaseItem,
  changeReleaseStatus,
  getRelease,
  getReleaseNotes,
  removeReleaseItem,
  updateRelease,
} from '../../api/release';
import { listRequirements } from '../../api/requirement';
import { listIssues } from '../../api/issue';
import type { ReleaseStatus, TargetType } from '../../types/common';
import { StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';
import { FullScreenLoader } from '../../components/FullScreenLoader';

const STATUS_OPTIONS: ReleaseStatus[] = ['PLANNED', 'IN_PROGRESS', 'RELEASED', 'ARCHIVED'];

export function ReleaseDetailPage() {
  const { projectId, releaseId } = useParams<{ projectId: string; releaseId: string }>();
  const id = Number(projectId);
  const rid = Number(releaseId);
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [itemType, setItemType] = useState<TargetType>('REQUIREMENT');
  const [itemTargetId, setItemTargetId] = useState('');
  const [showNotes, setShowNotes] = useState(false);

  const releaseQuery = useQuery({
    queryKey: ['release', rid],
    queryFn: () => getRelease(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const requirementsQuery = useQuery({
    queryKey: ['project', id, 'requirements', 'all'],
    queryFn: () => listRequirements(id, { size: 200 }),
    enabled: Number.isFinite(id) && itemType === 'REQUIREMENT',
  });

  const issuesQuery = useQuery({
    queryKey: ['project', id, 'issues', 'all'],
    queryFn: () => listIssues(id, { size: 200 }),
    enabled: Number.isFinite(id) && itemType === 'ISSUE',
  });

  const notesQuery = useQuery({
    queryKey: ['release', rid, 'notes'],
    queryFn: () => getReleaseNotes(id, rid),
    enabled: showNotes && Number.isFinite(id) && Number.isFinite(rid),
  });

  const [form, setForm] = useState({ version: '', name: '', releaseDate: '', description: '' });

  function startEdit() {
    if (!releaseQuery.data) return;
    const r = releaseQuery.data;
    setForm({ version: r.version, name: r.name ?? '', releaseDate: r.releaseDate ?? '', description: r.description ?? '' });
    setEditing(true);
  }

  const updateMutation = useMutation({
    mutationFn: () =>
      updateRelease(id, rid, {
        version: form.version,
        name: form.name || undefined,
        releaseDate: form.releaseDate || undefined,
        description: form.description || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['release', rid] });
      setEditing(false);
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: ReleaseStatus) => changeReleaseStatus(id, rid, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['release', rid] }),
  });

  const addItemMutation = useMutation({
    mutationFn: () => addReleaseItem(id, rid, itemType, Number(itemTargetId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['release', rid] });
      setItemTargetId('');
    },
  });

  const removeItemMutation = useMutation({
    mutationFn: (itemId: number) => removeReleaseItem(id, rid, itemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['release', rid] }),
  });

  if (releaseQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (releaseQuery.isError || !releaseQuery.data) {
    return <p className="text-sm text-red-600">릴리스를 불러오지 못했습니다.</p>;
  }

  const release = releaseQuery.data;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase text-slate-400">RELEASE</p>
          <h1 className="text-xl font-semibold text-slate-900">
            {release.version}
            {release.name ? ` — ${release.name}` : ''}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={release.status} />
          <button
            type="button"
            onClick={() => setShowNotes(true)}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-100"
          >
            릴리스 노트 미리보기
          </button>
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">상태</h2>
          <select
            value={release.status}
            onChange={(e) => statusMutation.mutate(e.target.value as ReleaseStatus)}
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
              <label className="mb-1 block text-sm text-slate-600">버전</label>
              <input
                type="text"
                value={form.version}
                onChange={(e) => setForm({ ...form, version: e.target.value })}
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
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-slate-400">릴리스 예정일</dt>
              <dd className="text-slate-700">{release.releaseDate ?? '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">설명</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{release.description || '-'}</dd>
            </div>
          </dl>
        )}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">포함된 요구사항/이슈</h2>
        <ul className="mb-3 flex flex-col gap-1.5">
          {release.items.map((item) => (
            <li key={item.id} className="flex items-center justify-between text-sm">
              <span className="flex items-center gap-2">
                <span className="inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                  {item.targetType}
                </span>
                <Link
                  to={item.targetType === 'REQUIREMENT'
                    ? `/projects/${id}/requirements/${item.targetId}`
                    : `/projects/${id}/issues/${item.targetId}`}
                  className="font-medium text-slate-700 hover:underline"
                >
                  {item.targetKey}
                </Link>
                {item.targetTitle}
                {item.targetStatus && <StatusBadge status={item.targetStatus} />}
              </span>
              <button
                type="button"
                onClick={() => removeItemMutation.mutate(item.id)}
                className="text-xs text-red-600 hover:underline"
              >
                제거
              </button>
            </li>
          ))}
          {release.items.length === 0 && <p className="text-sm text-slate-400">포함된 항목이 없습니다.</p>}
        </ul>
        <div className="flex gap-2">
          <select
            value={itemType}
            onChange={(e) => {
              setItemType(e.target.value as TargetType);
              setItemTargetId('');
            }}
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          >
            <option value="REQUIREMENT">요구사항</option>
            <option value="ISSUE">이슈</option>
          </select>
          <select
            value={itemTargetId}
            onChange={(e) => setItemTargetId(e.target.value)}
            className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          >
            <option value="">항목 선택</option>
            {itemType === 'REQUIREMENT'
              ? requirementsQuery.data?.content.map((r) => (
                  <option key={r.id} value={r.id}>{r.reqKey} - {r.title}</option>
                ))
              : issuesQuery.data?.content.map((i) => (
                  <option key={i.id} value={i.id}>{i.issueKey} - {i.title}</option>
                ))}
          </select>
          <button
            type="button"
            disabled={!itemTargetId || addItemMutation.isPending}
            onClick={() => addItemMutation.mutate()}
            className="rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800 disabled:opacity-50"
          >
            추가
          </button>
        </div>
      </div>

      {showNotes && (
        <Modal title="릴리스 노트 미리보기" onClose={() => setShowNotes(false)}>
          {notesQuery.isLoading ? (
            <p className="text-sm text-slate-400">불러오는 중...</p>
          ) : (
            <pre className="max-h-96 overflow-y-auto whitespace-pre-wrap text-sm text-slate-700">
              {notesQuery.data?.notes}
            </pre>
          )}
        </Modal>
      )}
    </div>
  );
}
