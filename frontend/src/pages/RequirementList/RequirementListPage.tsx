import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createRequirement, listRequirements } from '../../api/requirement';
import { listMembers } from '../../api/project';
import type { Priority, RequirementStatus, RequirementType } from '../../types/common';
import { PriorityBadge, StatusBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';

const STATUS_OPTIONS: RequirementStatus[] = ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED', 'REJECTED'];
const TYPE_OPTIONS: RequirementType[] = ['FUNCTIONAL', 'NON_FUNCTIONAL', 'BUSINESS'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function RequirementListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [priority, setPriority] = useState('');
  const [keyword, setKeyword] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const requirementsQuery = useQuery({
    queryKey: ['project', id, 'requirements', { status, type, priority, keyword }],
    queryFn: () =>
      listRequirements(id, {
        status: (status || undefined) as RequirementStatus | undefined,
        type: (type || undefined) as RequirementType | undefined,
        priority: (priority || undefined) as Priority | undefined,
        keyword: keyword || undefined,
        size: 100,
      }),
    enabled: Number.isFinite(id),
  });

  const membersQuery = useQuery({
    queryKey: ['project', id, 'members'],
    queryFn: () => listMembers(id),
    enabled: Number.isFinite(id),
  });

  const [form, setForm] = useState({
    title: '',
    description: '',
    type: 'FUNCTIONAL' as RequirementType,
    priority: 'MEDIUM' as Priority,
    parentRequirementId: '',
    assignedTo: '',
    dueDate: '',
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createRequirement(id, {
        title: form.title,
        description: form.description || undefined,
        type: form.type,
        priority: form.priority,
        parentRequirementId: form.parentRequirementId ? Number(form.parentRequirementId) : undefined,
        assignedTo: form.assignedTo ? Number(form.assignedTo) : undefined,
        dueDate: form.dueDate || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'requirements'] });
      setShowCreate(false);
      setForm({ title: '', description: '', type: 'FUNCTIONAL', priority: 'MEDIUM', parentRequirementId: '', assignedTo: '', dueDate: '' });
    },
  });

  const requirements = requirementsQuery.data?.content ?? [];

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">요구사항 목록</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
        >
          새 요구사항
        </button>
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
          <option value="">전체 상태</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <select value={type} onChange={(e) => setType(e.target.value)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
          <option value="">전체 유형</option>
          {TYPE_OPTIONS.map((t) => (
            <option key={t} value={t}>{t}</option>
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
              <th className="px-3 py-2">유형</th>
              <th className="px-3 py-2">우선순위</th>
              <th className="px-3 py-2">상태</th>
              <th className="px-3 py-2">담당자</th>
              <th className="px-3 py-2">상위 요구사항</th>
            </tr>
          </thead>
          <tbody>
            {requirements.map((r) => (
              <tr key={r.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-3 py-2">
                  <Link to={`/projects/${id}/requirements/${r.id}`} className="font-medium text-slate-700 hover:underline">
                    {r.reqKey}
                  </Link>
                </td>
                <td className="px-3 py-2">
                  {r.parentRequirementId && <span className="mr-1 text-slate-300">└</span>}
                  {r.title}
                </td>
                <td className="px-3 py-2">{r.type}</td>
                <td className="px-3 py-2"><PriorityBadge priority={r.priority} /></td>
                <td className="px-3 py-2"><StatusBadge status={r.status} /></td>
                <td className="px-3 py-2">{r.assignedToName ?? '-'}</td>
                <td className="px-3 py-2">{r.parentRequirementKey ?? '-'}</td>
              </tr>
            ))}
            {requirements.length === 0 && (
              <tr>
                <td colSpan={7} className="px-3 py-6 text-center text-slate-400">
                  요구사항이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <Modal title="새 요구사항" onClose={() => setShowCreate(false)}>
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
                rows={3}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3 grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm text-slate-600">유형</label>
                <select
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value as RequirementType })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  {TYPE_OPTIONS.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
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
            </div>
            <div className="mb-3 grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm text-slate-600">상위 요구사항</label>
                <select
                  value={form.parentRequirementId}
                  onChange={(e) => setForm({ ...form, parentRequirementId: e.target.value })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">없음</option>
                  {requirements.map((r) => (
                    <option key={r.id} value={r.id}>{r.reqKey}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-sm text-slate-600">담당자</label>
                <select
                  value={form.assignedTo}
                  onChange={(e) => setForm({ ...form, assignedTo: e.target.value })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">미지정</option>
                  {membersQuery.data?.map((m) => (
                    <option key={m.userId} value={m.userId}>{m.fullName}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="mb-4">
              <label className="mb-1 block text-sm text-slate-600">마감일</label>
              <input
                type="date"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
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
