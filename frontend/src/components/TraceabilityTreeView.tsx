import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getTraceabilityTree } from '../api/requirement';
import type { TraceabilityTreeDescendant } from '../types/requirement';
import { StatusBadge } from './Badge';
import { FullScreenLoader } from './FullScreenLoader';

function DescendantNode({ projectId, node, depth }: { projectId: number; node: TraceabilityTreeDescendant; depth: number }) {
  const [expanded, setExpanded] = useState(depth < 2);
  const hasChildren = node.children.length > 0;
  const uncovered = node.linkedIssues.length === 0;

  return (
    <div className="ml-4 border-l border-slate-200 pl-4">
      <div className="flex items-center gap-2 py-1.5">
        {hasChildren ? (
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            className="w-4 text-xs text-slate-400 hover:text-slate-700"
          >
            {expanded ? '▾' : '▸'}
          </button>
        ) : (
          <span className="w-4" />
        )}
        {uncovered && <span title="연결된 이슈 없음">⚠️</span>}
        <Link
          to={`/projects/${projectId}/requirements/${node.id}`}
          className="text-sm font-medium text-slate-800 hover:underline"
        >
          {node.reqKey}
        </Link>
        <span className="text-sm text-slate-600">{node.title}</span>
        <StatusBadge status={node.status} />
      </div>

      {expanded && node.linkedIssues.length > 0 && (
        <div className="ml-8 flex flex-col gap-1 pb-1">
          {node.linkedIssues.map((issue) => (
            <div key={issue.id} className="flex items-center gap-2 text-sm text-slate-500">
              <span className="text-blue-500">🔗</span>
              <span className="font-medium">{issue.issueKey}</span>
              <span>{issue.title}</span>
              <span className="text-xs text-slate-400">({issue.linkType})</span>
              <StatusBadge status={issue.status} />
            </div>
          ))}
        </div>
      )}

      {expanded && hasChildren && (
        <div>
          {node.children.map((child) => (
            <DescendantNode key={child.id} projectId={projectId} node={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

export function TraceabilityTreeView({ projectId, reqId }: { projectId: number; reqId: number }) {
  const treeQuery = useQuery({
    queryKey: ['requirement', reqId, 'traceability-tree'],
    queryFn: () => getTraceabilityTree(projectId, reqId),
  });

  if (treeQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (treeQuery.isError || !treeQuery.data) {
    return <p className="text-sm text-red-600">추적성 트리를 불러오지 못했습니다.</p>;
  }

  const tree = treeQuery.data;

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-1 text-sm text-slate-500">
        {tree.ancestors.map((ancestor) => (
          <span key={ancestor.id} className="flex items-center gap-1">
            <Link to={`/projects/${projectId}/requirements/${ancestor.id}`} className="hover:underline">
              {ancestor.reqKey}
            </Link>
            <span className="text-slate-300">›</span>
          </span>
        ))}
        <span className="font-semibold text-slate-800">
          {tree.self.reqKey}(현재)
        </span>
      </div>

      <div className="rounded-md border border-slate-200 bg-white p-3">
        <div className="flex items-center gap-2 pb-2">
          <span className="text-sm font-semibold text-slate-900">{tree.self.reqKey}</span>
          <span className="text-sm text-slate-600">{tree.self.title}</span>
          <StatusBadge status={tree.self.status} />
        </div>
        {tree.descendants.length === 0 ? (
          <p className="ml-4 text-sm text-slate-400">하위 요구사항이 없습니다.</p>
        ) : (
          tree.descendants.map((node) => (
            <DescendantNode key={node.id} projectId={projectId} node={node} depth={0} />
          ))
        )}
      </div>
    </div>
  );
}
