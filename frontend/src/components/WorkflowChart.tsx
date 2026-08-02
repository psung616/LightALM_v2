import { useEffect, useId, useRef, useState } from 'react';
import mermaid from 'mermaid';

interface WorkflowChartProps {
  mainStages: string[];
  branchStage?: string;
  current?: string;
  counts?: Record<string, number>;
}

let mermaidInitialized = false;
function ensureMermaidInitialized() {
  if (!mermaidInitialized) {
    mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose' });
    mermaidInitialized = true;
  }
}

function buildDefinition(mainStages: string[], branchStage: string | undefined, current: string | undefined, counts: Record<string, number> | undefined): string {
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

/**
 * §5.5 — 읽기 전용 상태 흐름 시각화. 상태 전이를 강제하지 않으며, 고정된 순서를 Mermaid stateDiagram-v2로 그린다.
 */
export function WorkflowChart({ mainStages, branchStage, current, counts }: WorkflowChartProps) {
  const rawId = useId();
  const idBase = rawId.replace(/[^a-zA-Z0-9]/g, '');
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState(false);

  const definition = buildDefinition(mainStages, branchStage, current, counts);

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
      <p className="mt-2 text-[11px] text-slate-400">참고용 흐름도이며 상태는 자유롭게 변경할 수 있습니다.</p>
    </div>
  );
}

export const REQUIREMENT_MAIN_STAGES = ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED'];
export const REQUIREMENT_BRANCH_STAGE = 'REJECTED';
export const ISSUE_MAIN_STAGES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED'];
