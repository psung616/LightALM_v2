import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { listRequirements, listRequirementChildren } from '../api/requirement';
import { listIssues } from '../api/issue';
import { listTestCases } from '../api/testCase';
import type { RequirementLevel } from '../types/common';
import type { Requirement } from '../types/requirement';

/**
 * ADR-013: 프로젝트 사이드바에 PRD/SRS/Defect/TestCase 유형별 트리를 보여주는 패널.
 * 전역 탐색기가 아니라 현재 진입한 프로젝트 안에서만 동작한다(05-frontend.md §5.22).
 * 모든 아코디언은 기본적으로 접힌 상태로 시작하고, 펼칠 때만 데이터를 지연 로딩한다.
 */

const PREVIEW_LIMIT = 10;

function AccordionHeader({
  label,
  count,
  open,
  onToggle,
}: {
  label: string;
  count: number | undefined;
  open: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className="flex w-full items-center justify-between rounded-md px-2 py-1.5 text-xs font-semibold uppercase text-slate-500 hover:bg-slate-100"
    >
      <span className="flex items-center gap-1.5">
        <span className="text-slate-300">{open ? '▾' : '▸'}</span>
        {label}
      </span>
      <span className="rounded-full bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-500">
        {count ?? '-'}
      </span>
    </button>
  );
}

function RequirementTreeNode({ projectId, node }: { projectId: number; node: Requirement }) {
  const [expanded, setExpanded] = useState(false);

  const childrenQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', 'requirement-children', node.id],
    queryFn: () => listRequirementChildren(projectId, node.id),
    enabled: expanded,
  });

  return (
    <li>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="w-3 shrink-0 text-[10px] text-slate-400"
          aria-label={expanded ? '접기' : '펼치기'}
        >
          {expanded ? '▾' : '▸'}
        </button>
        <Link
          to={`/projects/${projectId}/requirements/${node.id}`}
          title={node.title}
          className="truncate text-xs text-slate-600 hover:underline"
        >
          <span className="font-medium text-slate-500">{node.reqKey}</span> {node.title}
        </Link>
      </div>
      {expanded && (
        <ul className="ml-3 mt-1 flex flex-col gap-1 border-l border-slate-100 pl-2">
          {childrenQuery.isLoading && <li className="text-xs text-slate-400">불러오는 중...</li>}
          {childrenQuery.data?.map((child) => (
            <RequirementTreeNode key={child.id} projectId={projectId} node={child} />
          ))}
          {childrenQuery.data?.length === 0 && <li className="text-xs text-slate-300">하위 항목이 없습니다.</li>}
        </ul>
      )}
    </li>
  );
}

function RequirementLevelSection({
  projectId,
  level,
  label,
}: {
  projectId: number;
  level: RequirementLevel;
  label: string;
}) {
  const [open, setOpen] = useState(false);

  const countQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', level, 'count'],
    queryFn: () => listRequirements(projectId, { requirementLevel: level, size: 1 }),
  });

  const rootsQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', level, 'roots'],
    queryFn: () => listRequirements(projectId, { requirementLevel: level, rootOnly: true, size: 200 }),
    enabled: open,
  });

  return (
    <div>
      <AccordionHeader label={label} count={countQuery.data?.totalElements} open={open} onToggle={() => setOpen((v) => !v)} />
      {open && (
        <ul className="ml-2 mt-1 flex flex-col gap-1">
          {rootsQuery.isLoading && <li className="text-xs text-slate-400">불러오는 중...</li>}
          {rootsQuery.data?.content.map((node) => (
            <RequirementTreeNode key={node.id} projectId={projectId} node={node} />
          ))}
          {rootsQuery.data?.content.length === 0 && <li className="text-xs text-slate-300">항목이 없습니다.</li>}
        </ul>
      )}
    </div>
  );
}

function DefectSection({ projectId }: { projectId: number }) {
  const [open, setOpen] = useState(false);

  const countQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', 'defect', 'count'],
    queryFn: () => listIssues(projectId, { type: 'BUG', size: 1 }),
  });

  const previewQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', 'defect', 'preview'],
    queryFn: () => listIssues(projectId, { type: 'BUG', size: PREVIEW_LIMIT }),
    enabled: open,
  });

  const total = countQuery.data?.totalElements ?? 0;
  const items = previewQuery.data?.content ?? [];
  const remaining = Math.max(total - items.length, 0);

  return (
    <div>
      <AccordionHeader label="Defect" count={countQuery.data?.totalElements} open={open} onToggle={() => setOpen((v) => !v)} />
      {open && (
        <ul className="ml-2 mt-1 flex flex-col gap-1">
          {previewQuery.isLoading && <li className="text-xs text-slate-400">불러오는 중...</li>}
          {items.map((issue) => (
            <li key={issue.id}>
              <Link
                to={`/projects/${projectId}/issues/${issue.id}`}
                title={issue.title}
                className="block truncate text-xs text-slate-600 hover:underline"
              >
                <span className="font-medium text-slate-500">{issue.issueKey}</span> {issue.title}
              </Link>
            </li>
          ))}
          {items.length === 0 && !previewQuery.isLoading && <li className="text-xs text-slate-300">결함이 없습니다.</li>}
          {remaining > 0 && (
            <li>
              <Link to={`/projects/${projectId}/issues?type=BUG`} className="text-xs text-primary hover:underline">
                +{remaining}개 더보기 → 이슈 목록
              </Link>
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

function TestCaseSection({ projectId }: { projectId: number }) {
  const [open, setOpen] = useState(false);

  const countQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', 'test-case', 'count'],
    queryFn: () => listTestCases(projectId, { size: 1 }),
  });

  const previewQuery = useQuery({
    queryKey: ['project', projectId, 'work-item-tree', 'test-case', 'preview'],
    queryFn: () => listTestCases(projectId, { size: PREVIEW_LIMIT }),
    enabled: open,
  });

  const items = previewQuery.data?.content ?? [];

  return (
    <div>
      <AccordionHeader label="TestCase" count={countQuery.data?.totalElements} open={open} onToggle={() => setOpen((v) => !v)} />
      {open && (
        <ul className="ml-2 mt-1 flex flex-col gap-1">
          {previewQuery.isLoading && <li className="text-xs text-slate-400">불러오는 중...</li>}
          {items.map((tc) => (
            <li key={tc.id}>
              <Link
                to={`/projects/${projectId}/test-cases/${tc.id}`}
                title={tc.title}
                className="block truncate text-xs text-slate-600 hover:underline"
              >
                <span className="font-medium text-slate-500">{tc.tcKey}</span> {tc.title}
              </Link>
            </li>
          ))}
          {items.length === 0 && !previewQuery.isLoading && <li className="text-xs text-slate-300">테스트케이스가 없습니다.</li>}
          <li>
            <Link to={`/projects/${projectId}/test-cases`} className="text-xs text-primary hover:underline">
              전체 보기 → 테스트케이스 목록
            </Link>
          </li>
        </ul>
      )}
    </div>
  );
}

export function WorkItemTreePanel({ projectId }: { projectId: number }) {
  return (
    <div className="my-2 flex flex-col gap-1 border-y border-slate-200 py-2">
      <RequirementLevelSection projectId={projectId} level="PRD" label="PRD" />
      <RequirementLevelSection projectId={projectId} level="SRS" label="SRS" />
      <DefectSection projectId={projectId} />
      <TestCaseSection projectId={projectId} />
    </div>
  );
}
