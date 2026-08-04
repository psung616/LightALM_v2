import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { changeIssueStatus, getIssue, listIssueLinks, updateIssue } from '../../api/issue';
import { listComments, createComment } from '../../api/comment';
import { listGitLinks, createGitLink, listBuilds, triggerBuild } from '../../api/integration';
import { listAuditLogsForTarget } from '../../api/auditLog';
import type { IssueStatus, IssueType, Priority } from '../../types/common';
import { PriorityBadge, StatusBadge } from '../../components/Badge';
import { ISSUE_MAIN_STAGES, WorkflowChart } from '../../components/WorkflowChart';
import { FullScreenLoader } from '../../components/FullScreenLoader';
import { AuditLogList } from '../../components/AuditLogList';

const STATUS_OPTIONS: IssueStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED'];
const TYPE_OPTIONS: IssueType[] = ['BUG', 'TASK', 'STORY', 'IMPROVEMENT'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const BUILD_STATUS_COLORS: Record<string, string> = {
  SUCCESS: 'bg-green-100 text-green-700',
  FAILURE: 'bg-red-100 text-red-700',
  UNSTABLE: 'bg-amber-100 text-amber-700',
  RUNNING: 'bg-blue-100 text-blue-700',
  ABORTED: 'bg-slate-200 text-slate-600',
};

export function IssueDetailPage() {
  const { projectId, issueId } = useParams<{ projectId: string; issueId: string }>();
  const id = Number(projectId);
  const iid = Number(issueId);
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [gitInputMode, setGitInputMode] = useState<'COMMIT' | 'PULL_REQUEST'>('COMMIT');
  const [gitValue, setGitValue] = useState('');

  const issueQuery = useQuery({
    queryKey: ['issue', iid],
    queryFn: () => getIssue(id, iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const linksQuery = useQuery({
    queryKey: ['issue', iid, 'links'],
    queryFn: () => listIssueLinks(id, iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const commentsQuery = useQuery({
    queryKey: ['issues', iid, 'comments'],
    queryFn: () => listComments(id, 'issues', iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const gitLinksQuery = useQuery({
    queryKey: ['issues', iid, 'git-links'],
    queryFn: () => listGitLinks(id, 'issues', iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const buildsQuery = useQuery({
    queryKey: ['issues', iid, 'builds'],
    queryFn: () => listBuilds(id, 'issues', iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const auditLogsQuery = useQuery({
    queryKey: ['issues', iid, 'audit-logs'],
    queryFn: () => listAuditLogsForTarget(id, 'issues', iid),
    enabled: Number.isFinite(id) && Number.isFinite(iid),
  });

  const [form, setForm] = useState<{ title: string; description: string; type: IssueType; priority: Priority; dueDate: string }>({
    title: '',
    description: '',
    type: 'TASK',
    priority: 'MEDIUM',
    dueDate: '',
  });

  function startEdit() {
    if (!issueQuery.data) return;
    const i = issueQuery.data;
    setForm({ title: i.title, description: i.description ?? '', type: i.type, priority: i.priority, dueDate: i.dueDate ?? '' });
    setEditing(true);
  }

  const updateMutation = useMutation({
    mutationFn: () =>
      updateIssue(id, iid, {
        title: form.title,
        description: form.description || undefined,
        type: form.type,
        priority: form.priority,
        assigneeId: issueQuery.data?.assigneeId ?? undefined,
        dueDate: form.dueDate || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue', iid] });
      setEditing(false);
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: IssueStatus) => changeIssueStatus(id, iid, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issue', iid] }),
  });

  const commentMutation = useMutation({
    mutationFn: () => createComment(id, 'issues', iid, commentText),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', iid, 'comments'] });
      setCommentText('');
    },
  });

  const gitLinkMutation = useMutation({
    mutationFn: () =>
      createGitLink(
        id,
        'issues',
        iid,
        gitInputMode,
        gitInputMode === 'COMMIT' ? gitValue : undefined,
        gitInputMode === 'PULL_REQUEST' ? Number(gitValue) : undefined,
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', iid, 'git-links'] });
      setGitValue('');
    },
  });

  const triggerMutation = useMutation({
    mutationFn: () => triggerBuild(id, 'ISSUE', iid),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issues', iid, 'builds'] }),
  });

  if (issueQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (issueQuery.isError || !issueQuery.data) {
    return <p className="text-sm text-red-600">이슈를 불러오지 못했습니다.</p>;
  }

  const issue = issueQuery.data;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase text-slate-400">{issue.issueKey}</p>
          <h1 className="text-xl font-semibold text-slate-900">{issue.title}</h1>
        </div>
        <div className="flex items-center gap-2">
          <PriorityBadge priority={issue.priority} />
          <StatusBadge status={issue.status} />
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">상태 흐름</h2>
          <select
            value={issue.status}
            onChange={(e) => statusMutation.mutate(e.target.value as IssueStatus)}
            className="rounded-md border border-slate-300 px-2 py-1 text-sm"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
        <WorkflowChart mainStages={ISSUE_MAIN_STAGES} current={issue.status} />
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
                rows={4}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="mb-3 grid grid-cols-3 gap-3">
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
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => updateMutation.mutate()}
                disabled={updateMutation.isPending}
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
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
              <dt className="text-slate-400">설명</dt>
              <dd className="whitespace-pre-wrap text-slate-700">{issue.description || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">유형</dt>
              <dd className="text-slate-700">{issue.type}</dd>
            </div>
            <div>
              <dt className="text-slate-400">담당자</dt>
              <dd className="text-slate-700">{issue.assigneeName ?? '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">마감일</dt>
              <dd className="text-slate-700">{issue.dueDate ?? '-'}</dd>
            </div>
          </dl>
        )}
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">연결된 요구사항</h2>
        <ul className="flex flex-col gap-1.5">
          {linksQuery.data?.map((link) => (
            <li key={link.linkId} className="flex items-center justify-between text-sm">
              <span>
                <span className="mr-2 font-medium text-slate-700">{link.linkedKey}</span>
                {link.linkedTitle}
                <span className="ml-2 text-xs text-slate-400">({link.linkType})</span>
              </span>
              <StatusBadge status={link.linkedStatus} />
            </li>
          ))}
          {linksQuery.data?.length === 0 && <p className="text-sm text-slate-400">연결된 요구사항이 없습니다.</p>}
        </ul>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">GitHub 커밋/PR 연결</h2>
        <ul className="mb-3 flex flex-col gap-1.5">
          {gitLinksQuery.data?.map((g) => (
            <li key={g.id} className="text-sm">
              <a href={g.url} target="_blank" rel="noreferrer" className="text-primary hover:underline">
                {g.source === 'COMMIT' ? g.commitSha?.slice(0, 7) : `PR #${g.prNumber}`}
              </a>
              <span className="ml-2 text-slate-500">{g.message}</span>
            </li>
          ))}
          {gitLinksQuery.data?.length === 0 && <p className="text-sm text-slate-400">연결된 커밋/PR이 없습니다.</p>}
        </ul>
        <div className="flex gap-2">
          <select
            value={gitInputMode}
            onChange={(e) => setGitInputMode(e.target.value as 'COMMIT' | 'PULL_REQUEST')}
            className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          >
            <option value="COMMIT">커밋 SHA</option>
            <option value="PULL_REQUEST">PR 번호</option>
          </select>
          <input
            type="text"
            value={gitValue}
            onChange={(e) => setGitValue(e.target.value)}
            placeholder={gitInputMode === 'COMMIT' ? 'commit SHA' : 'PR 번호'}
            className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm"
          />
          <button
            type="button"
            disabled={!gitValue || gitLinkMutation.isPending}
            onClick={() => gitLinkMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            연결
          </button>
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">Jenkins 빌드 이력</h2>
          <button
            type="button"
            disabled={triggerMutation.isPending}
            onClick={() => triggerMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            빌드 트리거
          </button>
        </div>
        <ul className="flex flex-col gap-1.5">
          {buildsQuery.data?.map((b) => (
            <li key={b.id} className="flex items-center justify-between text-sm">
              <a href={b.buildUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">
                {b.jobName} #{b.buildNumber}
              </a>
              <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${BUILD_STATUS_COLORS[b.status] ?? 'bg-slate-100 text-slate-600'}`}>
                {b.status}
              </span>
            </li>
          ))}
          {buildsQuery.data?.length === 0 && <p className="text-sm text-slate-400">빌드 이력이 없습니다.</p>}
        </ul>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">댓글</h2>
        <ul className="mb-3 flex flex-col gap-3">
          {commentsQuery.data?.map((c) => (
            <li key={c.id} className="text-sm">
              <p className="font-medium text-slate-700">{c.authorName}</p>
              <p className="whitespace-pre-wrap text-slate-600">{c.content}</p>
            </li>
          ))}
        </ul>
        <div className="flex gap-2">
          <input
            type="text"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder="댓글을 입력하세요"
            className="flex-1 rounded-md border border-slate-300 px-3 py-1.5 text-sm"
          />
          <button
            type="button"
            disabled={!commentText || commentMutation.isPending}
            onClick={() => commentMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            등록
          </button>
        </div>
      </div>

      <div className="mt-6 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">이력</h2>
        <AuditLogList logs={auditLogsQuery.data ?? []} />
      </div>
    </div>
  );
}
