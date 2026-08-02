interface WorkflowChartProps {
  mainStages: string[];
  branchStage?: string;
  current: string;
  counts?: Record<string, number>;
  labels?: Record<string, string>;
}

/**
 * §5.5 — 읽기 전용 상태 흐름 시각화. 상태 전이를 강제하지 않으며, 고정된 순서를 그대로 보여준다.
 */
export function WorkflowChart({ mainStages, branchStage, current, counts, labels }: WorkflowChartProps) {
  const label = (status: string) => labels?.[status] ?? status;

  return (
    <div>
      <div className="flex items-center gap-1 overflow-x-auto">
        {mainStages.map((stage, i) => (
          <div key={stage} className="flex items-center gap-1">
            <div
              className={`flex flex-col items-center rounded-md border px-3 py-2 text-xs whitespace-nowrap ${
                stage === current
                  ? 'border-slate-900 bg-slate-900 text-white'
                  : 'border-slate-300 bg-white text-slate-600'
              }`}
            >
              <span>{label(stage)}</span>
              {counts && <span className="mt-0.5 text-[11px] opacity-80">{counts[stage] ?? 0}건</span>}
            </div>
            {i < mainStages.length - 1 && <span className="text-slate-300">→</span>}
          </div>
        ))}
      </div>
      {branchStage && (
        <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
          <span className="text-slate-300">⇢ (어디서든 전이 가능)</span>
          <div
            className={`flex flex-col items-center rounded-md border px-3 py-2 text-xs whitespace-nowrap ${
              branchStage === current
                ? 'border-red-600 bg-red-600 text-white'
                : 'border-red-200 bg-red-50 text-red-500'
            }`}
          >
            <span>{label(branchStage)}</span>
            {counts && <span className="mt-0.5 text-[11px] opacity-80">{counts[branchStage] ?? 0}건</span>}
          </div>
        </div>
      )}
      <p className="mt-2 text-[11px] text-slate-400">참고용 흐름도이며 상태는 자유롭게 변경할 수 있습니다.</p>
    </div>
  );
}

export const REQUIREMENT_MAIN_STAGES = ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED'];
export const REQUIREMENT_BRANCH_STAGE = 'REJECTED';
export const ISSUE_MAIN_STAGES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED'];
