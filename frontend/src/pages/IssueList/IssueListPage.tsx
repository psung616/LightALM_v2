import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { changeIssueStatus, createIssue, listIssues } from '../../api/issue';
import { listMembers } from '../../api/project';
import type { IssueStatus, IssueType, Priority } from '../../types/common';
import type { Issue } from '../../types/issue';
import { PriorityBadge } from '../../components/Badge';
import { Modal } from '../../components/Modal';
import { ISSUE_MAIN_STAGES } from '../../components/WorkflowChart';

const TYPE_OPTIONS: IssueType[] = ['BUG', 'TASK', 'STORY', 'IMPROVEMENT'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUS_LABELS: Record<string, string> = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN PROGRESS',
  IN_REVIEW: 'IN REVIEW',
  DONE: 'DONE',
  CLOSED: 'CLOSED',
};

export function IssueListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();

  const [view, setView] = useState<'kanban' | 'table'>('kanban');
  const [keyword, setKeyword] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const issuesQuery = useQuery({
    queryKey: ['project', id, 'issues', { keyword }],
    queryFn: () => listIssues(id, { keyword: keyword || undefined, size: 200 }),
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
    type: 'TASK' as IssueType,
    priority: 'MEDIUM' as Priority,
    assigneeId: '',
    dueDate: '',
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createIssue(id, {
        title: form.title,
        description: form.description || undefined,
        type: form.type,
        priority: form.priority,
        assigneeId: form.assigneeId ? Number(form.assigneeId) : undefined,
        dueDate: form.dueDate || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id, 'issues'] });
      setShowCreate(false);
      setForm({ title: '', description: '', type: 'TASK', priority: 'MEDIUM', assigneeId: '', dueDate: '' });
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ issueId, status }: { issueId: number; status: IssueStatus }) => changeIssueStatus(id, issueId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', id, 'issues'] }),
  });

  const issues = issuesQuery.data?.content ?? [];
  const byStatus = (status: string) => issues.filter((i) => i.status === status);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">이슈 목록</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover"
        >
          새 이슈
        </button>
      </div>

      <div className="mb-4 flex items-center justify-between">
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="키워드 검색"
          className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
        />
        <div className="flex rounded-md border border-slate-300 text-sm">
          <button
            type="button"
            onClick={() => setView('kanban')}
            className={`px-3 py-1.5 ${view === 'kanban' ? 'bg-primary text-white' : 'text-slate-600'}`}
          >
            칸반
          </button>
          <button
            type="button"
            onClick={() => setView('table')}
            className={`px-3 py-1.5 ${view === 'table' ? 'bg-primary text-white' : 'text-slate-600'}`}
          >
            테이블
          </button>
        </div>
      </div>

      {view === 'kanban' ? (
        <div className="grid grid-cols-5 gap-3">
          {ISSUE_MAIN_STAGES.map((status) => (
            <div key={status} className="rounded-lg border border-slate-200 bg-slate-50 p-2">
              <p className="mb-2 text-xs font-semibold uppercase text-slate-500">
                {STATUS_LABELS[status]} ({byStatus(status).length})
              </p>
              <div className="flex flex-col gap-2">
                {byStatus(status).map((issue) => (
                  <IssueCard key={issue.id} projectId={id} issue={issue} onMove={(s) => statusMutation.mutate({ issueId: issue.id, status: s })} />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
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
              </tr>
            </thead>
            <tbody>
              {issues.map((issue) => (
                <tr key={issue.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                  <td className="px-3 py-2">
                    <Link to={`/projects/${id}/issues/${issue.id}`} className="font-medium text-slate-700 hover:underline">
                      {issue.issueKey}
                    </Link>
                  </td>
                  <td className="px-3 py-2">{issue.title}</td>
                  <td className="px-3 py-2">{issue.type}</td>
                  <td className="px-3 py-2"><PriorityBadge priority={issue.priority} /></td>
                  <td className="px-3 py-2">{STATUS_LABELS[issue.status]}</td>
                  <td className="px-3 py-2">{issue.assigneeName ?? '-'}</td>
                </tr>
              ))}
              {issues.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-3 py-6 text-center text-slate-400">이슈가 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <Modal title="새 이슈" onClose={() => setShowCreate(false)}>
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
                  onChange={(e) => setForm({ ...form, type: e.target.value as IssueType })}
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
            <div className="mb-4 grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm text-slate-600">담당자</label>
                <select
                  value={form.assigneeId}
                  onChange={(e) => setForm({ ...form, assigneeId: e.target.value })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="">미지정</option>
                  {membersQuery.data?.map((m) => (
                    <option key={m.userId} value={m.userId}>{m.fullName}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-sm text-slate-600">마감일</label>
                <input
                  type="date"
                  value={form.dueDate}
                  onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
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

function IssueCard({ projectId, issue, onMove }: { projectId: number; issue: Issue; onMove: (status: IssueStatus) => void }) {
  return (
    <div className="rounded-md border border-slate-200 bg-white p-2 shadow-sm">
      <Link to={`/projects/${projectId}/issues/${issue.id}`} className="text-xs font-medium text-slate-500 hover:underline">
        {issue.issueKey}
      </Link>
      <p className="mb-2 text-sm text-slate-800">{issue.title}</p>
      <div className="mb-2 flex items-center justify-between">
        <PriorityBadge priority={issue.priority} />
        <span className="text-xs text-slate-400">{issue.assigneeName ?? '미지정'}</span>
      </div>
      <select
        value={issue.status}
        onChange={(e) => onMove(e.target.value as IssueStatus)}
        className="w-full rounded border border-slate-200 px-1.5 py-1 text-xs"
      >
        {ISSUE_MAIN_STAGES.map((s) => (
          <option key={s} value={s}>{STATUS_LABELS[s]}</option>
        ))}
      </select>
    </div>
  );
}
