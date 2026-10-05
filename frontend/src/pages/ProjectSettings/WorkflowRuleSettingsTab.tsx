import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createWorkflowTransitionRule,
  deleteWorkflowTransitionRule,
  listWorkflowTransitionRules,
} from '../../api/workflow';
import type { TargetType, ProjectRole } from '../../types/common';
import type { WorkflowTransitionRule } from '../../types/workflow';
import { PROJECT_ROLE_DISPLAY_NAMES, SYSTEM_ROLE_DISPLAY_NAMES } from '../../auth/roleDisplayNames';

/**
 * ADR-012 §D.6(Phase 23) 설정 화면. target_type(요구사항/이슈만, 테스트케이스는 범위 밖 — §D.1)
 * 선택 → 상태 전이 매트릭스(행=from, 열=to) 체크박스 UI + 체크한 셀마다 최소 역할 선택.
 *
 * 범용 워크플로우 엔진이 아니다 — "이 상태에서 저 상태로 전이 가능한가(참/거짓) + 최소 역할"만
 * 다룬다. 역할을 바꾸려면(PUT 엔드포인트가 없음, §D.4) 기존 규칙을 삭제하고 새 역할로 다시
 * 생성한다.
 */
const STATUS_OPTIONS: Record<TargetType, string[]> = {
  REQUIREMENT: ['DRAFT', 'APPROVED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED', 'REJECTED'],
  ISSUE: ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CLOSED'],
};

const TARGET_TYPE_OPTIONS: [TargetType, string][] = [
  ['REQUIREMENT', '요구사항'],
  ['ISSUE', '이슈'],
];

/**
 * 새로 고를 수 있는 "최소 역할" 옵션(value는 API 값 그대로, 라벨만 표시명). ADR-015 P3: VIEWER(Project User)는
 * 읽기 전용이라 상태 전이를 할 수 없으므로(changeStatus의 MEMBER+ 사전 검사) 새 선택지에서 뺀다.
 * DB/API는 VIEWER 값을 계속 허용하므로, 이미 VIEWER로 저장된 규칙에만 LEGACY_VIEWER_OPTION을 덧붙여 표시한다.
 */
const ROLE_OPTIONS: [string, string][] = [
  ['', `${PROJECT_ROLE_DISPLAY_NAMES.MEMBER}+ (기본값)`],
  ['MEMBER', `${PROJECT_ROLE_DISPLAY_NAMES.MEMBER}+`],
  ['PROJECT_ADMIN', `${PROJECT_ROLE_DISPLAY_NAMES.PROJECT_ADMIN}+`],
];

const LEGACY_VIEWER_OPTION: [string, string] = [
  'VIEWER',
  `${PROJECT_ROLE_DISPLAY_NAMES.VIEWER}+ (실제로는 ${PROJECT_ROLE_DISPLAY_NAMES.MEMBER}+와 동일)`,
];

function roleOptionsFor(rule: WorkflowTransitionRule): [string, string][] {
  return rule.allowedRole === 'VIEWER' ? [...ROLE_OPTIONS, LEGACY_VIEWER_OPTION] : ROLE_OPTIONS;
}

export function WorkflowRuleSettingsTab({ projectId }: { projectId: number }) {
  const [targetType, setTargetType] = useState<TargetType>('REQUIREMENT');

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-4 flex gap-1 border-b border-slate-200 text-sm">
        {TARGET_TYPE_OPTIONS.map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setTargetType(key)}
            className={`px-3 py-2 ${targetType === key ? 'border-b-2 border-slate-900 font-medium text-slate-900' : 'text-slate-500'}`}
          >
            {label}
          </button>
        ))}
      </div>
      <WorkflowRuleMatrix key={targetType} projectId={projectId} targetType={targetType} />
    </div>
  );
}

function WorkflowRuleMatrix({ projectId, targetType }: { projectId: number; targetType: TargetType }) {
  const queryClient = useQueryClient();
  const queryKey = ['project', projectId, 'config', 'workflow-rules', targetType];
  const rulesQuery = useQuery({
    queryKey,
    queryFn: () => listWorkflowTransitionRules(projectId, targetType),
  });

  const createMutation = useMutation({
    mutationFn: (variables: { fromStatus: string; toStatus: string; allowedRole: ProjectRole | null }) =>
      createWorkflowTransitionRule(projectId, {
        targetType,
        fromStatus: variables.fromStatus,
        toStatus: variables.toStatus,
        allowedRole: variables.allowedRole,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: number) => deleteWorkflowTransitionRule(projectId, ruleId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  if (rulesQuery.isLoading) {
    return <p className="text-sm text-slate-400">불러오는 중...</p>;
  }
  if (rulesQuery.isError || !rulesQuery.data) {
    return <p className="text-sm text-red-600">규칙을 불러오지 못했습니다.</p>;
  }

  const statuses = STATUS_OPTIONS[targetType];
  const ruleByPair = new Map<string, WorkflowTransitionRule>();
  for (const rule of rulesQuery.data.rules) {
    ruleByPair.set(`${rule.fromStatus}|${rule.toStatus}`, rule);
  }

  function toggleCell(fromStatus: string, toStatus: string) {
    const existing = ruleByPair.get(`${fromStatus}|${toStatus}`);
    if (existing) {
      deleteMutation.mutate(existing.id);
    } else {
      createMutation.mutate({ fromStatus, toStatus, allowedRole: null });
    }
  }

  function changeRole(rule: WorkflowTransitionRule, allowedRole: string) {
    // PUT 엔드포인트가 없어(§D.4) 삭제 후 새 역할로 재생성한다.
    deleteMutation.mutate(rule.id, {
      onSuccess: () => {
        createMutation.mutate({
          fromStatus: rule.fromStatus,
          toStatus: rule.toStatus,
          allowedRole: (allowedRole || null) as ProjectRole | null,
        });
      },
    });
  }

  return (
    <div>
      {rulesQuery.data.freeTransitionMode && (
        <p className="mb-4 rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
          현재 자유 전이 모드입니다 — 규칙을 추가하면 그 순간부터 여기 표시되지 않은 전이는 차단됩니다. ({SYSTEM_ROLE_DISPLAY_NAMES.ADMIN}과
          이 프로젝트의 {PROJECT_ROLE_DISPLAY_NAMES.PROJECT_ADMIN}은 규칙과 무관하게 항상 모든 전이가 가능합니다.)
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr>
              <th className="border border-slate-200 bg-slate-50 px-2 py-1.5 text-left">from \ to</th>
              {statuses.map((to) => (
                <th key={to} className="border border-slate-200 bg-slate-50 px-2 py-1.5 text-center">
                  {to}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {statuses.map((from) => (
              <tr key={from}>
                <th className="border border-slate-200 bg-slate-50 px-2 py-1.5 text-left">{from}</th>
                {statuses.map((to) => {
                  if (from === to) {
                    return <td key={to} className="border border-slate-200 bg-slate-100 px-2 py-1.5 text-center text-slate-300">—</td>;
                  }
                  const rule = ruleByPair.get(`${from}|${to}`);
                  return (
                    <td key={to} className="border border-slate-200 px-2 py-1.5 text-center">
                      <div className="flex flex-col items-center gap-1">
                        <input
                          type="checkbox"
                          checked={!!rule}
                          onChange={() => toggleCell(from, to)}
                          disabled={createMutation.isPending || deleteMutation.isPending}
                        />
                        {rule && (
                          <select
                            value={rule.allowedRole ?? ''}
                            onChange={(e) => changeRole(rule, e.target.value)}
                            className="rounded border border-slate-300 px-1 py-0.5 text-[10px]"
                          >
                            {roleOptionsFor(rule).map(([value, label]) => (
                              <option key={value} value={value}>{label}</option>
                            ))}
                          </select>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {(createMutation.isError || deleteMutation.isError) && (
        <p className="mt-3 text-sm text-red-600">규칙 저장/삭제에 실패했습니다.</p>
      )}
    </div>
  );
}
