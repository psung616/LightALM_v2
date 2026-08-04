import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createLink, deleteLink, getMatrix } from '../../api/traceability';
import { listRequirements } from '../../api/requirement';
import { TraceabilityTreeView } from '../../components/TraceabilityTreeView';
import { FullScreenLoader } from '../../components/FullScreenLoader';
import type { TraceabilityMatrix } from '../../types/traceability';

export function TraceabilityPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);
  const queryClient = useQueryClient();
  const [view, setView] = useState<'matrix' | 'tree'>('matrix');
  const [treeRootId, setTreeRootId] = useState<number | null>(null);

  const matrixQuery = useQuery({
    queryKey: ['project', id, 'traceability-matrix'],
    queryFn: () => getMatrix(id),
    enabled: Number.isFinite(id) && view === 'matrix',
  });

  const requirementsQuery = useQuery({
    queryKey: ['project', id, 'requirements', 'roots'],
    queryFn: () => listRequirements(id, { size: 200 }),
    enabled: Number.isFinite(id),
  });

  const topLevelRequirements = useMemo(
    () => requirementsQuery.data?.content.filter((r) => !r.parentRequirementId) ?? [],
    [requirementsQuery.data],
  );

  const linkMutation = useMutation({
    mutationFn: ({ reqId, issueId }: { reqId: number; issueId: number }) =>
      createLink(id, { sourceType: 'REQUIREMENT', sourceId: reqId, targetType: 'ISSUE', targetId: issueId, linkType: 'IMPLEMENTS' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', id, 'traceability-matrix'] }),
  });

  const unlinkMutation = useMutation({
    mutationFn: (linkId: number) => deleteLink(id, linkId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', id, 'traceability-matrix'] }),
  });

  const activeTreeRoot = treeRootId ?? topLevelRequirements[0]?.id ?? null;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">추적성</h1>
        <div className="flex rounded-md border border-slate-300 text-sm">
          <button
            type="button"
            onClick={() => setView('matrix')}
            className={`px-3 py-1.5 ${view === 'matrix' ? 'bg-primary text-white' : 'text-slate-600'}`}
          >
            매트릭스 뷰
          </button>
          <button
            type="button"
            onClick={() => setView('tree')}
            className={`px-3 py-1.5 ${view === 'tree' ? 'bg-primary text-white' : 'text-slate-600'}`}
          >
            트리 뷰
          </button>
        </div>
      </div>

      {view === 'matrix' ? (
        matrixQuery.isLoading ? (
          <FullScreenLoader />
        ) : matrixQuery.isError || !matrixQuery.data ? (
          <p className="text-sm text-red-600">매트릭스를 불러오지 못했습니다.</p>
        ) : (
          <MatrixView
            matrix={matrixQuery.data}
            onToggle={(reqId, issueId, existingLinkId) => {
              if (existingLinkId) {
                unlinkMutation.mutate(existingLinkId);
              } else {
                linkMutation.mutate({ reqId, issueId });
              }
            }}
          />
        )
      ) : (
        <div>
          <div className="mb-4">
            <label className="mr-2 text-sm text-slate-600">기준 요구사항</label>
            <select
              value={activeTreeRoot ?? ''}
              onChange={(e) => setTreeRootId(Number(e.target.value))}
              className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
            >
              {topLevelRequirements.map((r) => (
                <option key={r.id} value={r.id}>{r.reqKey} - {r.title}</option>
              ))}
            </select>
          </div>
          {activeTreeRoot ? (
            <TraceabilityTreeView projectId={id} reqId={activeTreeRoot} />
          ) : (
            <p className="text-sm text-slate-400">최상위 요구사항이 없습니다.</p>
          )}
        </div>
      )}
    </div>
  );
}

function MatrixView({
  matrix,
  onToggle,
}: {
  matrix: TraceabilityMatrix;
  onToggle: (reqId: number, issueId: number, existingLinkId: number | null) => void;
}) {
  const uncoveredCount = matrix.requirements.filter(
    (r) => !matrix.links.some((l) => l.requirementId === r.id),
  ).length;

  return (
    <div>
      <p className="mb-3 text-sm text-slate-500">
        연결되지 않은 요구사항: <span className="font-semibold text-amber-600">{uncoveredCount}건</span>
      </p>
      <div className="overflow-auto rounded-lg border border-slate-200 bg-white">
        <table className="text-sm">
          <thead>
            <tr>
              <th className="sticky left-0 border-b border-r border-slate-200 bg-slate-50 px-3 py-2 text-left">요구사항 \ 이슈</th>
              {matrix.issues.map((issue) => (
                <th key={issue.id} className="border-b border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium whitespace-nowrap">
                  {issue.issueKey}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {matrix.requirements.map((req) => {
              const covered = matrix.links.some((l) => l.requirementId === req.id);
              return (
                <tr key={req.id}>
                  <td
                    className={`sticky left-0 border-b border-r border-slate-200 px-3 py-2 text-left whitespace-nowrap ${
                      covered ? 'bg-white' : 'bg-amber-50'
                    }`}
                  >
                    {!covered && <span className="mr-1">⚠️</span>}
                    {req.reqKey}
                  </td>
                  {matrix.issues.map((issue) => {
                    const link = matrix.links.find((l) => l.requirementId === req.id && l.issueId === issue.id);
                    return (
                      <td key={issue.id} className="border-b border-slate-200 px-3 py-2 text-center">
                        <button
                          type="button"
                          onClick={() => onToggle(req.id, issue.id, link?.id ?? null)}
                          className={`h-5 w-5 rounded ${link ? 'bg-primary' : 'bg-slate-100 hover:bg-slate-200'}`}
                          title={link ? link.linkType : '연결 없음 (클릭하여 연결)'}
                        />
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
