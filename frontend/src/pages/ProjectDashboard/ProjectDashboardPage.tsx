import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getProjectDashboardSummary } from '../../api/dashboard';
import { StatusBadge } from '../../components/Badge';
import {
  ISSUE_MAIN_STAGES,
  REQUIREMENT_BRANCH_STAGE,
  REQUIREMENT_MAIN_STAGES,
  WorkflowChart,
} from '../../components/WorkflowChart';
import { FullScreenLoader } from '../../components/FullScreenLoader';

export function ProjectDashboardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const [view, setView] = useState<'count' | 'workflow'>('count');

  const summaryQuery = useQuery({
    queryKey: ['project', id, 'dashboard-summary'],
    queryFn: () => getProjectDashboardSummary(id),
    enabled: Number.isFinite(id),
  });

  if (summaryQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (summaryQuery.isError || !summaryQuery.data) {
    return <p className="text-sm text-red-600">대시보드를 불러오지 못했습니다.</p>;
  }

  const summary = summaryQuery.data;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">프로젝트 대시보드</h1>
        <div className="flex rounded-md border border-slate-300 text-sm">
          <button
            type="button"
            onClick={() => setView('count')}
            className={`px-3 py-1.5 ${view === 'count' ? 'bg-slate-900 text-white' : 'text-slate-600'}`}
          >
            카운트 뷰
          </button>
          <button
            type="button"
            onClick={() => setView('workflow')}
            className={`px-3 py-1.5 ${view === 'workflow' ? 'bg-slate-900 text-white' : 'text-slate-600'}`}
          >
            Workflow 뷰
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">요구사항 상태</h2>
          {view === 'count' ? (
            <ul className="flex flex-col gap-1.5">
              {Object.entries(summary.requirementCountsByStatus).map(([status, count]) => (
                <li key={status} className="flex items-center justify-between text-sm">
                  <StatusBadge status={status} />
                  <span className="font-medium text-slate-700">{count}건</span>
                </li>
              ))}
            </ul>
          ) : (
            <WorkflowChart
              mainStages={REQUIREMENT_MAIN_STAGES}
              branchStage={REQUIREMENT_BRANCH_STAGE}
              current=""
              counts={summary.requirementCountsByStatus}
            />
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">이슈 상태</h2>
          {view === 'count' ? (
            <ul className="flex flex-col gap-1.5">
              {Object.entries(summary.issueCountsByStatus).map(([status, count]) => (
                <li key={status} className="flex items-center justify-between text-sm">
                  <StatusBadge status={status} />
                  <span className="font-medium text-slate-700">{count}건</span>
                </li>
              ))}
            </ul>
          ) : (
            <WorkflowChart mainStages={ISSUE_MAIN_STAGES} current="" counts={summary.issueCountsByStatus} />
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">최근 요구사항</h2>
          <ul className="flex flex-col gap-2">
            {summary.recentRequirements.map((r) => (
              <li key={r.id}>
                <Link to={`/projects/${id}/requirements/${r.id}`} className="flex items-center justify-between text-sm hover:underline">
                  <span>
                    <span className="mr-2 font-medium text-slate-700">{r.reqKey}</span>
                    {r.title}
                  </span>
                  <StatusBadge status={r.status} />
                </Link>
              </li>
            ))}
            {summary.recentRequirements.length === 0 && <p className="text-sm text-slate-400">데이터가 없습니다.</p>}
          </ul>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">최근 이슈</h2>
          <ul className="flex flex-col gap-2">
            {summary.recentIssues.map((i) => (
              <li key={i.id}>
                <Link to={`/projects/${id}/issues/${i.id}`} className="flex items-center justify-between text-sm hover:underline">
                  <span>
                    <span className="mr-2 font-medium text-slate-700">{i.issueKey}</span>
                    {i.title}
                  </span>
                  <StatusBadge status={i.status} />
                </Link>
              </li>
            ))}
            {summary.recentIssues.length === 0 && <p className="text-sm text-slate-400">데이터가 없습니다.</p>}
          </ul>
        </section>
      </div>
    </div>
  );
}
