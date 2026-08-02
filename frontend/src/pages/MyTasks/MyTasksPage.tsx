import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQueries, useQuery } from '@tanstack/react-query';
import { getMyDashboard } from '../../api/dashboard';
import { useAuth } from '../../auth/AuthContext';
import { listIssues } from '../../api/issue';
import { listRequirements } from '../../api/requirement';
import { StatusBadge } from '../../components/Badge';
import { FullScreenLoader } from '../../components/FullScreenLoader';

export function MyTasksPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<'issues' | 'requirements'>('issues');

  const dashboardQuery = useQuery({ queryKey: ['me', 'dashboard'], queryFn: getMyDashboard });

  const byProject = dashboardQuery.data?.byProject ?? [];

  const issueLists = useQueries({
    queries: byProject.map((p) => ({
      queryKey: ['project', p.projectId, 'my-issues', user?.id],
      queryFn: () => listIssues(p.projectId, { assigneeId: user!.id, size: 100 }),
      enabled: !!user,
    })),
  });

  const requirementLists = useQueries({
    queries: byProject.map((p) => ({
      queryKey: ['project', p.projectId, 'my-requirements', user?.id],
      queryFn: () => listRequirements(p.projectId, { assignedTo: user!.id, size: 100 }),
      enabled: !!user,
    })),
  });

  if (dashboardQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (dashboardQuery.isError || !dashboardQuery.data) {
    return <p className="text-sm text-red-600">내 작업을 불러오지 못했습니다.</p>;
  }

  const dashboard = dashboardQuery.data;
  const projectKeyToId = Object.fromEntries(dashboard.byProject.map((p) => [p.projectKey, p.projectId]));

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="mb-6 text-xl font-semibold text-slate-900">내 작업</h1>

      <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">이슈 상태별</h2>
          <ul className="flex flex-col gap-1.5">
            {Object.entries(dashboard.assignedIssuesByStatus).map(([status, count]) => (
              <li key={status} className="flex items-center justify-between text-sm">
                <StatusBadge status={status} />
                <span className="font-medium text-slate-700">{count}건</span>
              </li>
            ))}
          </ul>
        </section>
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">요구사항 상태별</h2>
          <ul className="flex flex-col gap-1.5">
            {Object.entries(dashboard.assignedRequirementsByStatus).map(([status, count]) => (
              <li key={status} className="flex items-center justify-between text-sm">
                <StatusBadge status={status} />
                <span className="font-medium text-slate-700">{count}건</span>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-red-200 bg-red-50 p-4">
          <h2 className="mb-3 text-sm font-semibold text-red-700">기한 초과</h2>
          <ul className="flex flex-col gap-1.5">
            {dashboard.overdue.map((item) => (
              <AssignedItemRow key={`${item.type}-${item.id}`} item={item} projectId={projectKeyToId[item.projectKey]} />
            ))}
            {dashboard.overdue.length === 0 && <p className="text-sm text-red-400">기한 초과 항목이 없습니다.</p>}
          </ul>
        </section>
        <section className="rounded-lg border border-amber-200 bg-amber-50 p-4">
          <h2 className="mb-3 text-sm font-semibold text-amber-700">마감 임박 (7일 이내)</h2>
          <ul className="flex flex-col gap-1.5">
            {dashboard.dueSoon.map((item) => (
              <AssignedItemRow key={`${item.type}-${item.id}`} item={item} projectId={projectKeyToId[item.projectKey]} />
            ))}
            {dashboard.dueSoon.length === 0 && <p className="text-sm text-amber-500">마감 임박 항목이 없습니다.</p>}
          </ul>
        </section>
      </div>

      <section className="mb-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">프로젝트별 담당 항목</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {dashboard.byProject.map((p) => (
            <Link
              key={p.projectId}
              to={`/projects/${p.projectId}`}
              className="rounded-lg border border-slate-200 bg-white p-3 hover:border-slate-400"
            >
              <p className="text-xs font-medium uppercase text-slate-400">{p.projectKey}</p>
              <p className="mb-2 truncate text-sm font-semibold text-slate-800">{p.projectName}</p>
              <p className="text-xs text-slate-500">이슈 {p.assignedIssueCount}건 · 요구사항 {p.assignedRequirementCount}건</p>
            </Link>
          ))}
          {dashboard.byProject.length === 0 && <p className="text-sm text-slate-400">소속된 프로젝트가 없습니다.</p>}
        </div>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 flex gap-1 border-b border-slate-200 text-sm">
          <button
            type="button"
            onClick={() => setTab('issues')}
            className={`px-3 py-2 ${tab === 'issues' ? 'border-b-2 border-slate-900 font-medium text-slate-900' : 'text-slate-500'}`}
          >
            할당된 이슈
          </button>
          <button
            type="button"
            onClick={() => setTab('requirements')}
            className={`px-3 py-2 ${tab === 'requirements' ? 'border-b-2 border-slate-900 font-medium text-slate-900' : 'text-slate-500'}`}
          >
            할당된 요구사항
          </button>
        </div>

        {tab === 'issues' ? (
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="py-2">키</th>
                <th className="py-2">제목</th>
                <th className="py-2">프로젝트</th>
                <th className="py-2">상태</th>
              </tr>
            </thead>
            <tbody>
              {issueLists.flatMap((q, idx) =>
                (q.data?.content ?? []).map((issue) => (
                  <tr key={issue.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-2">
                      <Link to={`/projects/${byProject[idx].projectId}/issues/${issue.id}`} className="font-medium text-slate-700 hover:underline">
                        {issue.issueKey}
                      </Link>
                    </td>
                    <td className="py-2">{issue.title}</td>
                    <td className="py-2">{byProject[idx].projectKey}</td>
                    <td className="py-2"><StatusBadge status={issue.status} /></td>
                  </tr>
                )),
              )}
            </tbody>
          </table>
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="py-2">키</th>
                <th className="py-2">제목</th>
                <th className="py-2">프로젝트</th>
                <th className="py-2">상태</th>
              </tr>
            </thead>
            <tbody>
              {requirementLists.flatMap((q, idx) =>
                (q.data?.content ?? []).map((req) => (
                  <tr key={req.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-2">
                      <Link to={`/projects/${byProject[idx].projectId}/requirements/${req.id}`} className="font-medium text-slate-700 hover:underline">
                        {req.reqKey}
                      </Link>
                    </td>
                    <td className="py-2">{req.title}</td>
                    <td className="py-2">{byProject[idx].projectKey}</td>
                    <td className="py-2"><StatusBadge status={req.status} /></td>
                  </tr>
                )),
              )}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

function AssignedItemRow({
  item,
  projectId,
}: {
  item: { type: string; id: number; key: string; title: string; projectKey: string; dueDate: string; status: string };
  projectId: number | undefined;
}) {
  const path = item.type === 'ISSUE' ? 'issues' : 'requirements';
  const content = (
    <>
      <span className="mr-1 text-xs text-slate-400">[{item.projectKey}]</span>
      <span className="font-medium text-slate-700">{item.key}</span> {item.title}
    </>
  );
  return (
    <li className="flex items-center justify-between text-sm">
      {projectId ? (
        <Link to={`/projects/${projectId}/${path}/${item.id}`} className="hover:underline">
          {content}
        </Link>
      ) : (
        <span>{content}</span>
      )}
      <span className="text-xs">{item.dueDate}</span>
    </li>
  );
}
