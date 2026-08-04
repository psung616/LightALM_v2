import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  changeRequirementStatus,
  getRequirement,
  listRequirementChildren,
  listRequirementLinks,
  updateRequirement,
} from '../../api/requirement';
import { listIssues } from '../../api/issue';
import { createLink } from '../../api/traceability';
import { listComments, createComment } from '../../api/comment';
import { listGitLinks, createGitLink } from '../../api/integration';
import { listTestCasesForRequirement } from '../../api/testCase';
import { listAuditLogsForTarget } from '../../api/auditLog';
import { createApprovalRequest } from '../../api/approval';
import type { LinkType, Priority, RequirementStatus, RequirementType } from '../../types/common';
import { PriorityBadge, StatusBadge } from '../../components/Badge';
import { REQUIREMENT_BRANCH_STAGE, REQUIREMENT_MAIN_STAGES, WorkflowChart } from '../../components/WorkflowChart';
import { TraceabilityTreeView } from '../../components/TraceabilityTreeView';
import { FullScreenLoader } from '../../components/FullScreenLoader';
import { AuditLogList } from '../../components/AuditLogList';

const STATUS_OPTIONS: RequirementStatus[] = ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED', 'REJECTED'];
const TYPE_OPTIONS: RequirementType[] = ['FUNCTIONAL', 'NON_FUNCTIONAL', 'BUSINESS'];
const PRIORITY_OPTIONS: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const LINK_TYPE_OPTIONS: LinkType[] = ['IMPLEMENTS', 'TESTS', 'DEPENDS_ON', 'RELATES_TO', 'DUPLICATES'];

export function RequirementDetailPage() {
  const { projectId, reqId } = useParams<{ projectId: string; reqId: string }>();
  const id = Number(projectId);
  const rid = Number(reqId);
  const queryClient = useQueryClient();

  const [showTree, setShowTree] = useState(false);
  const [editing, setEditing] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [linkIssueId, setLinkIssueId] = useState('');
  const [linkType, setLinkType] = useState<LinkType>('IMPLEMENTS');
  const [gitInputMode, setGitInputMode] = useState<'COMMIT' | 'PULL_REQUEST'>('COMMIT');
  const [gitValue, setGitValue] = useState('');

  const requirementQuery = useQuery({
    queryKey: ['requirement', rid],
    queryFn: () => getRequirement(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const childrenQuery = useQuery({
    queryKey: ['requirement', rid, 'children'],
    queryFn: () => listRequirementChildren(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const linksQuery = useQuery({
    queryKey: ['requirement', rid, 'links'],
    queryFn: () => listRequirementLinks(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const commentsQuery = useQuery({
    queryKey: ['requirements', rid, 'comments'],
    queryFn: () => listComments(id, 'requirements', rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const gitLinksQuery = useQuery({
    queryKey: ['requirements', rid, 'git-links'],
    queryFn: () => listGitLinks(id, 'requirements', rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const issuesQuery = useQuery({
    queryKey: ['project', id, 'issues', 'all'],
    queryFn: () => listIssues(id, { size: 200 }),
    enabled: Number.isFinite(id),
  });

  const testCasesQuery = useQuery({
    queryKey: ['requirement', rid, 'test-cases'],
    queryFn: () => listTestCasesForRequirement(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const auditLogsQuery = useQuery({
    queryKey: ['requirements', rid, 'audit-logs'],
    queryFn: () => listAuditLogsForTarget(id, 'requirements', rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const [form, setForm] = useState<{ title: string; description: string; type: RequirementType; priority: Priority; dueDate: string }>({
    title: '',
    description: '',
    type: 'FUNCTIONAL',
    priority: 'MEDIUM',
    dueDate: '',
  });

  function startEdit() {
    if (!requirementQuery.data) return;
    const r = requirementQuery.data;
    setForm({ title: r.title, description: r.description ?? '', type: r.type, priority: r.priority, dueDate: r.dueDate ?? '' });
    setEditing(true);
  }

  const updateMutation = useMutation({
    mutationFn: () =>
      updateRequirement(id, rid, {
        title: form.title,
        description: form.description || undefined,
        type: form.type,
        priority: form.priority,
        parentRequirementId: requirementQuery.data?.parentRequirementId ?? undefined,
        assignedTo: requirementQuery.data?.assignedToId ?? undefined,
        dueDate: form.dueDate || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirement', rid] });
      setEditing(false);
    },
  });

  const statusMutation = useMutation({
    mutationFn: (status: RequirementStatus) => changeRequirementStatus(id, rid, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['requirement', rid] }),
  });

  const approvalRequestMutation = useMutation({
    mutationFn: () => createApprovalRequest(id, rid, 'APPROVED'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['requirement', rid] }),
  });

  const linkMutation = useMutation({
    mutationFn: () =>
      createLink(id, { sourceType: 'REQUIREMENT', sourceId: rid, targetType: 'ISSUE', targetId: Number(linkIssueId), linkType }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirement', rid, 'links'] });
      setLinkIssueId('');
    },
  });

  const commentMutation = useMutation({
    mutationFn: () => createComment(id, 'requirements', rid, commentText),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirements', rid, 'comments'] });
      setCommentText('');
    },
  });

  const gitLinkMutation = useMutation({
    mutationFn: () =>
      createGitLink(
        id,
        'requirements',
        rid,
        gitInputMode,
        gitInputMode === 'COMMIT' ? gitValue : undefined,
        gitInputMode === 'PULL_REQUEST' ? Number(gitValue) : undefined,
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['requirements', rid, 'git-links'] });
      setGitValue('');
    },
  });

  if (requirementQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (requirementQuery.isError || !requirementQuery.data) {
    return <p className="text-sm text-red-600">요구사항을 불러오지 못했습니다.</p>;
  }

  const r = requirementQuery.data;

  if (showTree) {
    return (
      <div>
        <button type="button" onClick={() => setShowTree(false)} className="mb-4 text-sm text-slate-500 hover:underline">
          ← 상세로 돌아가기
        </button>
        <TraceabilityTreeView projectId={id} reqId={rid} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase text-slate-400">{r.reqKey}</p>
          <h1 className="text-xl font-semibold text-slate-900">{r.title}</h1>
        </div>
        <div className="flex items-center gap-2">
          <PriorityBadge priority={r.priority} />
          <StatusBadge status={r.status} />
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">상태 흐름</h2>
          <div className="flex items-center gap-2">
            {r.status === 'DRAFT' && (
              <button
                type="button"
                disabled={approvalRequestMutation.isPending || approvalRequestMutation.isSuccess}
                onClick={() => approvalRequestMutation.mutate()}
                className="rounded-md border border-slate-300 px-3 py-1 text-sm text-slate-700 hover:bg-slate-100 disabled:opacity-50"
              >
                {approvalRequestMutation.isSuccess ? '승인 대기 중' : '승인 요청'}
              </button>
            )}
            <select
              value={r.status}
              onChange={(e) => statusMutation.mutate(e.target.value as RequirementStatus)}
              className="rounded-md border border-slate-300 px-2 py-1 text-sm"
            >
              {STATUS_OPTIONS.filter((s) => !(r.status === 'DRAFT' && s === 'APPROVED')).map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
        <WorkflowChart mainStages={REQUIREMENT_MAIN_STAGES} branchStage={REQUIREMENT_BRANCH_STAGE} current={r.status} />
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
              <dd className="whitespace-pre-wrap text-slate-700">{r.description || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">유형</dt>
              <dd className="text-slate-700">{r.type}</dd>
            </div>
            <div>
              <dt className="text-slate-400">담당자</dt>
              <dd className="text-slate-700">{r.assignedToName ?? '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-400">마감일</dt>
              <dd className="text-slate-700">{r.dueDate ?? '-'}</dd>
            </div>
          </dl>
        )}
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">하위 요구사항</h2>
          <button type="button" onClick={() => setShowTree(true)} className="text-sm text-primary hover:underline">
            추적성 트리로 보기
          </button>
        </div>
        <ul className="flex flex-col gap-1.5">
          {childrenQuery.data?.map((c) => (
            <li key={c.id}>
              <Link to={`/projects/${id}/requirements/${c.id}`} className="flex items-center justify-between text-sm hover:underline">
                <span>
                  <span className="mr-2 font-medium text-slate-700">{c.reqKey}</span>
                  {c.title}
                </span>
                <StatusBadge status={c.status} />
              </Link>
            </li>
          ))}
          {childrenQuery.data?.length === 0 && <p className="text-sm text-slate-400">하위 요구사항이 없습니다.</p>}
        </ul>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-700">연결된 테스트케이스</h2>
          <Link to={`/projects/${id}/test-cases`} className="text-sm text-primary hover:underline">
            테스트케이스 만들기
          </Link>
        </div>
        <ul className="flex flex-col gap-1.5">
          {testCasesQuery.data?.map((tc) => (
            <li key={tc.id}>
              <Link to={`/projects/${id}/test-cases/${tc.id}`} className="flex items-center justify-between text-sm hover:underline">
                <span>
                  <span className="mr-2 font-medium text-slate-700">{tc.tcKey}</span>
                  {tc.title}
                </span>
                <StatusBadge status={tc.status} />
              </Link>
            </li>
          ))}
          {testCasesQuery.data?.length === 0 && <p className="text-sm text-slate-400">연결된 테스트케이스가 없습니다.</p>}
        </ul>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">연결된 이슈/요구사항</h2>
        <ul className="mb-3 flex flex-col gap-1.5">
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
          {linksQuery.data?.length === 0 && <p className="text-sm text-slate-400">연결된 항목이 없습니다.</p>}
        </ul>
        <div className="flex gap-2">
          <select value={linkIssueId} onChange={(e) => setLinkIssueId(e.target.value)} className="flex-1 rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            <option value="">이슈 선택</option>
            {issuesQuery.data?.content.map((i) => (
              <option key={i.id} value={i.id}>{i.issueKey} - {i.title}</option>
            ))}
          </select>
          <select value={linkType} onChange={(e) => setLinkType(e.target.value as LinkType)} className="rounded-md border border-slate-300 px-2 py-1.5 text-sm">
            {LINK_TYPE_OPTIONS.map((lt) => (
              <option key={lt} value={lt}>{lt}</option>
            ))}
          </select>
          <button
            type="button"
            disabled={!linkIssueId || linkMutation.isPending}
            onClick={() => linkMutation.mutate()}
            className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover disabled:opacity-50"
          >
            이슈 연결
          </button>
        </div>
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
