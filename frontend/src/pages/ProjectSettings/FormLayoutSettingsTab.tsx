import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getFormLayoutForConfig, saveFormLayout } from '../../api/formLayout';
import { listActiveCustomFieldDefinitions } from '../../api/customField';
import type { FormLayoutTargetType, SaveFormLayoutFieldRequest, SaveFormLayoutRequest } from '../../types/formLayout';
import type { CustomFieldDefinitionSummary } from '../../types/customField';

/**
 * ADR-012 §B.4(Phase 21) 설정 화면. target_type별로 섹션 추가/삭제, 섹션 내 필드 순서를
 * 상/하 버튼으로 변경(드래그앤드롭 라이브러리는 과설계 방지를 위해 추가하지 않음), 노출 토글.
 *
 * {@link STANDARD_FIELD_OPTIONS}는 백엔드 StandardFieldKeyRegistry의 화이트리스트를 화면
 * 표시용으로 그대로 옮긴 것이다 — "모든 가능한 표준 필드 키" 자체를 돌려주는 API가 없어서
 * (ADR §B.3은 3개 엔드포인트만 정의) 프론트에 동일 목록을 중복 보관한다. 백엔드 레지스트리가
 * 바뀌면 이 목록도 같이 갱신해야 한다(구현 시 추가한 알려진 동기화 포인트).
 */
const STANDARD_FIELD_OPTIONS: Record<FormLayoutTargetType, { key: string; label: string }[]> = {
  REQUIREMENT: [
    { key: 'title', label: '제목' },
    { key: 'description', label: '설명' },
    { key: 'type', label: '유형' },
    { key: 'priority', label: '우선순위' },
    { key: 'requirementLevel', label: '문서 레벨(PRD/SRS)' },
    { key: 'parentRequirementId', label: '상위 요구사항' },
    { key: 'assignedTo', label: '담당자' },
    { key: 'dueDate', label: '마감일' },
  ],
  ISSUE: [
    { key: 'title', label: '제목' },
    { key: 'description', label: '설명' },
    { key: 'type', label: '유형' },
    { key: 'priority', label: '우선순위' },
    { key: 'assigneeId', label: '담당자' },
    { key: 'dueDate', label: '마감일' },
  ],
  TEST_CASE: [
    { key: 'title', label: '제목' },
    { key: 'description', label: '설명' },
    { key: 'preconditions', label: '사전조건' },
    { key: 'steps', label: '실행 절차' },
    { key: 'expectedResult', label: '예상 결과' },
    { key: 'priority', label: '우선순위' },
    { key: 'requirementId', label: '연관 요구사항' },
    { key: 'status', label: '상태' },
  ],
};

interface DraftField extends SaveFormLayoutFieldRequest {
  standardFieldLabel?: string | null;
  customFieldLabel?: string | null;
}

interface DraftSection {
  title: string;
  fields: DraftField[];
}

const TARGET_TYPE_OPTIONS: [FormLayoutTargetType, string][] = [
  ['REQUIREMENT', '요구사항'],
  ['ISSUE', '이슈'],
  ['TEST_CASE', '테스트케이스'],
];

export function FormLayoutSettingsTab({ projectId }: { projectId: number }) {
  const [targetType, setTargetType] = useState<FormLayoutTargetType>('REQUIREMENT');

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
      {/* key로 target_type마다 완전히 새 상태로 리마운트 — 탭 전환 시 이전 draft가 섞이는 것을 방지 */}
      <FormLayoutEditor key={targetType} projectId={projectId} targetType={targetType} />
    </div>
  );
}

function FormLayoutEditor({ projectId, targetType }: { projectId: number; targetType: FormLayoutTargetType }) {
  const queryClient = useQueryClient();
  const configQueryKey = ['project', projectId, 'config', 'form-layouts', targetType];
  const configQuery = useQuery({
    queryKey: configQueryKey,
    queryFn: () => getFormLayoutForConfig(projectId, targetType),
  });

  const customFieldsQuery = useQuery({
    queryKey: ['project', projectId, 'custom-fields', targetType, 'active'],
    queryFn: () => listActiveCustomFieldDefinitions(projectId, targetType),
  });

  const [draft, setDraft] = useState<DraftSection[] | null>(null);

  useEffect(() => {
    if (configQuery.data) {
      setDraft(
        configQuery.data.sections.map((section) => ({
          title: section.title ?? '기본 정보',
          fields: section.fields.map((f) => ({
            fieldSource: f.fieldSource,
            standardFieldKey: f.standardFieldKey,
            customFieldId: f.customFieldId,
            displayOrder: f.displayOrder,
            visible: f.visible,
            standardFieldLabel: f.standardFieldLabel,
            customFieldLabel: f.customFieldLabel,
          })),
        })),
      );
    }
  }, [configQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => {
      const request: SaveFormLayoutRequest = {
        sections: (draft ?? []).map((section, sectionIndex) => ({
          title: section.title,
          displayOrder: sectionIndex,
          fields: section.fields.map((field, fieldIndex) => ({
            fieldSource: field.fieldSource,
            standardFieldKey: field.fieldSource === 'STANDARD' ? field.standardFieldKey : null,
            customFieldId: field.fieldSource === 'CUSTOM' ? field.customFieldId : null,
            displayOrder: fieldIndex,
            visible: field.visible,
          })),
        })),
      };
      return saveFormLayout(projectId, targetType, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: configQueryKey });
      queryClient.invalidateQueries({ queryKey: ['project', projectId, 'form-layouts', targetType] });
    },
  });

  if (configQuery.isLoading || !draft) {
    return <p className="text-sm text-slate-400">불러오는 중...</p>;
  }

  const placedStandardKeys = new Set(
    draft.flatMap((s) => s.fields.filter((f) => f.fieldSource === 'STANDARD').map((f) => f.standardFieldKey)),
  );
  const placedCustomFieldIds = new Set(
    draft.flatMap((s) => s.fields.filter((f) => f.fieldSource === 'CUSTOM').map((f) => f.customFieldId)),
  );
  const availableStandardOptions = STANDARD_FIELD_OPTIONS[targetType].filter((opt) => !placedStandardKeys.has(opt.key));
  const availableCustomOptions = (customFieldsQuery.data ?? []).filter((cf) => !placedCustomFieldIds.has(cf.id));

  function updateSection(index: number, updater: (section: DraftSection) => DraftSection) {
    setDraft((prev) => prev && prev.map((s, i) => (i === index ? updater(s) : s)));
  }

  function addSection() {
    setDraft((prev) => [...(prev ?? []), { title: '새 섹션', fields: [] }]);
  }

  function removeSection(index: number) {
    setDraft((prev) => prev && prev.filter((_, i) => i !== index));
  }

  function moveSection(index: number, direction: -1 | 1) {
    setDraft((prev) => {
      if (!prev) return prev;
      const next = [...prev];
      const target = index + direction;
      if (target < 0 || target >= next.length) return prev;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  function addStandardField(sectionIndex: number, option: { key: string; label: string }) {
    updateSection(sectionIndex, (section) => ({
      ...section,
      fields: [
        ...section.fields,
        {
          fieldSource: 'STANDARD',
          standardFieldKey: option.key,
          customFieldId: null,
          displayOrder: section.fields.length,
          visible: true,
          standardFieldLabel: option.label,
        },
      ],
    }));
  }

  function addCustomField(sectionIndex: number, cf: CustomFieldDefinitionSummary) {
    updateSection(sectionIndex, (section) => ({
      ...section,
      fields: [
        ...section.fields,
        {
          fieldSource: 'CUSTOM',
          standardFieldKey: null,
          customFieldId: cf.id,
          displayOrder: section.fields.length,
          visible: true,
          customFieldLabel: cf.label,
        },
      ],
    }));
  }

  function removeField(sectionIndex: number, fieldIndex: number) {
    updateSection(sectionIndex, (section) => ({
      ...section,
      fields: section.fields.filter((_, i) => i !== fieldIndex),
    }));
  }

  function moveField(sectionIndex: number, fieldIndex: number, direction: -1 | 1) {
    updateSection(sectionIndex, (section) => {
      const target = fieldIndex + direction;
      if (target < 0 || target >= section.fields.length) return section;
      const fields = [...section.fields];
      [fields[fieldIndex], fields[target]] = [fields[target], fields[fieldIndex]];
      return { ...section, fields };
    });
  }

  function toggleVisible(sectionIndex: number, fieldIndex: number) {
    updateSection(sectionIndex, (section) => ({
      ...section,
      fields: section.fields.map((f, i) => (i === fieldIndex ? { ...f, visible: !f.visible } : f)),
    }));
  }

  return (
    <div>
      {configQuery.data?.id == null && (
        <p className="mb-4 rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
          아직 이 유형에 대한 레이아웃을 설정하지 않았습니다. 지금 보이는 목록은 표준 필드의 기본 순서(기본
          레이아웃)입니다 — 저장하기 전까지 생성/수정 화면은 기존과 동일하게 보입니다.
        </p>
      )}

      {draft.map((section, sectionIndex) => (
        <div key={sectionIndex} className="mb-4 rounded-md border border-slate-200 p-3">
          <div className="mb-2 flex items-center gap-2">
            <input
              type="text"
              value={section.title}
              onChange={(e) => updateSection(sectionIndex, (s) => ({ ...s, title: e.target.value }))}
              className="flex-1 rounded-md border border-slate-300 px-2 py-1 text-sm font-medium"
            />
            <button type="button" onClick={() => moveSection(sectionIndex, -1)} className="rounded border border-slate-300 px-2 py-1 text-xs">
              ↑
            </button>
            <button type="button" onClick={() => moveSection(sectionIndex, 1)} className="rounded border border-slate-300 px-2 py-1 text-xs">
              ↓
            </button>
            <button type="button" onClick={() => removeSection(sectionIndex)} className="text-xs text-red-500 hover:underline">
              섹션 삭제
            </button>
          </div>

          <table className="mb-2 w-full text-sm">
            <tbody>
              {section.fields.map((field, fieldIndex) => (
                <tr key={fieldIndex} className="border-b border-slate-100 last:border-0">
                  <td className="py-1.5">
                    <span className="mr-1 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-500">
                      {field.fieldSource === 'STANDARD' ? '표준' : '커스텀'}
                    </span>
                    {field.fieldSource === 'STANDARD' ? field.standardFieldLabel ?? field.standardFieldKey : field.customFieldLabel}
                    {field.fieldSource === 'CUSTOM' && (
                      <span className="ml-1 text-xs text-amber-600">(생성/수정 화면에는 아직 미반영)</span>
                    )}
                  </td>
                  <td className="py-1.5 text-right">
                    <label className="mr-3 inline-flex items-center gap-1 text-xs text-slate-600">
                      <input type="checkbox" checked={field.visible} onChange={() => toggleVisible(sectionIndex, fieldIndex)} />
                      노출
                    </label>
                    <button type="button" onClick={() => moveField(sectionIndex, fieldIndex, -1)} className="mr-1 rounded border border-slate-300 px-1.5 py-0.5 text-xs">
                      ↑
                    </button>
                    <button type="button" onClick={() => moveField(sectionIndex, fieldIndex, 1)} className="mr-1 rounded border border-slate-300 px-1.5 py-0.5 text-xs">
                      ↓
                    </button>
                    <button type="button" onClick={() => removeField(sectionIndex, fieldIndex)} className="text-xs text-red-500 hover:underline">
                      제거
                    </button>
                  </td>
                </tr>
              ))}
              {section.fields.length === 0 && (
                <tr>
                  <td colSpan={2} className="py-2 text-center text-xs text-slate-400">
                    이 섹션에 배치된 필드가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          <div className="flex flex-wrap gap-2">
            {availableStandardOptions.length > 0 && (
              <select
                defaultValue=""
                onChange={(e) => {
                  const opt = STANDARD_FIELD_OPTIONS[targetType].find((o) => o.key === e.target.value);
                  if (opt) addStandardField(sectionIndex, opt);
                  e.target.value = '';
                }}
                className="rounded-md border border-slate-300 px-2 py-1 text-xs"
              >
                <option value="">+ 표준 필드 추가</option>
                {availableStandardOptions.map((opt) => (
                  <option key={opt.key} value={opt.key}>{opt.label}</option>
                ))}
              </select>
            )}
            {availableCustomOptions.length > 0 && (
              <select
                defaultValue=""
                onChange={(e) => {
                  const cf = availableCustomOptions.find((o) => String(o.id) === e.target.value);
                  if (cf) addCustomField(sectionIndex, cf);
                  e.target.value = '';
                }}
                className="rounded-md border border-slate-300 px-2 py-1 text-xs"
              >
                <option value="">+ 커스텀 필드 추가</option>
                {availableCustomOptions.map((cf) => (
                  <option key={cf.id} value={cf.id}>{cf.label}</option>
                ))}
              </select>
            )}
          </div>
          {availableCustomOptions.length > 0 && (
            <p className="mt-1 text-xs text-slate-400">
              커스텀 필드 배치는 저장/조회에는 정상 반영되지만, 생성/수정 화면에는 아직 반영되지 않습니다 — 추후
              지원 예정입니다. 지금은 상세 화면의 "커스텀 필드" 영역에서 계속 입력/수정할 수 있습니다.
            </p>
          )}
        </div>
      ))}

      <div className="mb-4 flex items-center justify-between">
        <button type="button" onClick={addSection} className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-100">
          + 섹션 추가
        </button>
        <button
          type="button"
          onClick={() => saveMutation.mutate()}
          disabled={saveMutation.isPending}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
        >
          저장
        </button>
      </div>
      {saveMutation.isError && (
        <p className="text-sm text-red-600">
          저장에 실패했습니다. 표준 필드는 최소 1개 이상 노출돼야 하고, 섹션 안에 최소 1개의 필드가 있어야 합니다.
        </p>
      )}
      {saveMutation.isSuccess && <p className="text-sm text-green-600">저장되었습니다.</p>}
    </div>
  );
}
