import { useEffect, useId, useRef, useState } from 'react';
import mermaid from 'mermaid';
import { getWorkflowNextStatuses } from '../api/workflow';
import type { TargetType } from '../types/common';

interface WorkflowChartProps {
  mainStages: string[];
  branchStage?: string;
  current?: string;
  counts?: Record<string, number>;
  /**
   * ADR-012 §D.5(2026-10-05 갱신) — projectId/targetType을 함께 넘기면 그 프로젝트의
   * workflow_transition_rules(§3.29)를 조회해 화이트리스트가 설정돼 있으면 그 화살표만,
   * 설정이 없으면(대다수 프로젝트) 기존처럼 고정된 전체 자유 전이 다이어그램을 그린다.
   * 생략하면 항상 기존 고정 다이어그램(회귀 없음)을 그린다.
   */
  projectId?: number;
  targetType?: TargetType;
}

let mermaidInitialized = false;
function ensureMermaidInitialized() {
  if (!mermaidInitialized) {
    mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose' });
    mermaidInitialized = true;
  }
}

function buildFixedDefinition(mainStages: string[], branchStage: string | undefined, current: string | undefined, counts: Record<string, number> | undefined): string {
  const lines: string[] = ['stateDiagram-v2', `    [*] --> ${mainStages[0]}`];

  for (let i = 0; i < mainStages.length - 1; i++) {
    lines.push(`    ${mainStages[i]} --> ${mainStages[i + 1]}`);
  }

  if (branchStage) {
    for (const stage of mainStages) {
      lines.push(`    ${stage} --> ${branchStage}`);
    }
  }

  if (counts) {
    const allStages = branchStage ? [...mainStages, branchStage] : mainStages;
    for (const stage of allStages) {
      lines.push(`    ${stage} : ${stage} (${counts[stage] ?? 0}건)`);
    }
  }

  lines.push('    classDef current fill:#0f172a,color:#ffffff,stroke:#0f172a');
  if (current) {
    lines.push(`    class ${current} current`);
  }
  if (branchStage) {
    lines.push('    classDef rejected fill:#fef2f2,color:#dc2626,stroke:#fca5a5');
    lines.push(`    class ${branchStage} rejected`);
  }

  return lines.join('\n');
}

function buildWhitelistDefinition(nodes: string[], edges: [string, string][], current: string | undefined, counts: Record<string, number> | undefined): string {
  const lines: string[] = ['stateDiagram-v2'];
  for (const node of nodes) {
    lines.push(`    state ${node}`);
  }
  for (const [from, to] of edges) {
    lines.push(`    ${from} --> ${to}`);
  }

  if (counts) {
    for (const stage of nodes) {
      lines.push(`    ${stage} : ${stage} (${counts[stage] ?? 0}건)`);
    }
  }

  lines.push('    classDef current fill:#0f172a,color:#ffffff,stroke:#0f172a');
  if (current) {
    lines.push(`    class ${current} current`);
  }

  return lines.join('\n');
}

/**
 * §5.5 — 상태 흐름 시각화. projectId/targetType이 없으면(대부분의 기존 호출부) 상태 전이를
 * 강제하지 않는 순수 시각화로 고정된 순서를 Mermaid stateDiagram-v2로 그린다(회귀 없음).
 * projectId/targetType이 있으면 workflow_transition_rules 조회 결과에 따라 자유 전이
 * 다이어그램 또는 화이트리스트 다이어그램을 동적으로 그린다(ADR-012 §D.5).
 */
export function WorkflowChart({ mainStages, branchStage, current, counts, projectId, targetType }: WorkflowChartProps) {
  const rawId = useId();
  const idBase = rawId.replace(/[^a-zA-Z0-9]/g, '');
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState(false);
  const [dynamicEdges, setDynamicEdges] = useState<[string, string][] | null>(null);
  const [freeTransitionMode, setFreeTransitionMode] = useState(true);

  const allNodes = branchStage ? [...mainStages, branchStage] : mainStages;
  const nodesKey = allNodes.join(',');

  useEffect(() => {
    if (!projectId || !targetType) {
      setDynamicEdges(null);
      setFreeTransitionMode(true);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const results = await Promise.all(
          allNodes.map((status) => getWorkflowNextStatuses(projectId, targetType, status)),
        );
        if (cancelled) return;
        const isFree = results.every((r) => r.freeTransitionMode);
        setFreeTransitionMode(isFree);
        if (isFree) {
          setDynamicEdges(null);
        } else {
          const edges: [string, string][] = [];
          results.forEach((result, index) => {
            const from = allNodes[index];
            result.nextStatuses.forEach((to) => edges.push([from, to]));
          });
          setDynamicEdges(edges);
        }
      } catch {
        if (!cancelled) {
          setDynamicEdges(null);
          setFreeTransitionMode(true);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
    // nodesKey가 바뀌지 않는 한(mainStages/branchStage 동일) 재조회할 필요가 없다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, targetType, nodesKey]);

  const definition = dynamicEdges
    ? buildWhitelistDefinition(allNodes, dynamicEdges, current, counts)
    : buildFixedDefinition(mainStages, branchStage, current, counts);

  useEffect(() => {
    ensureMermaidInitialized();
    let cancelled = false;
    setError(false);
    mermaid
      .render(`workflow-${idBase}`, definition)
      .then(({ svg }) => {
        if (!cancelled && containerRef.current) {
          containerRef.current.innerHTML = svg;
        }
      })
      .catch(() => {
        if (!cancelled) setError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [definition, idBase]);

  return (
    <div>
      <div ref={containerRef} className="overflow-x-auto" />
      {error && <p className="text-xs text-red-500">다이어그램을 렌더링하지 못했습니다.</p>}
      <p className="mt-2 text-[11px] text-slate-400">
        {freeTransitionMode
          ? '참고용 흐름도이며 상태는 자유롭게 변경할 수 있습니다.'
          : '이 프로젝트는 워크플로우 규칙이 설정되어 있어 표시된 화살표로만 상태를 변경할 수 있습니다.'}
      </p>
    </div>
  );
}

export const REQUIREMENT_MAIN_STAGES = ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED'];
export const REQUIREMENT_BRANCH_STAGE = 'REJECTED';
export const ISSUE_MAIN_STAGES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED'];
