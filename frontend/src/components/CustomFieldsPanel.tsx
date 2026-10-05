import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listCustomFieldValues, saveCustomFieldValues } from '../api/customField';
import type { CustomFieldTargetPathSegment, CustomFieldValue } from '../types/customField';

/**
 * ADR-012 §A.5 / 05-frontend.md §5.18: 요구사항/이슈/테스트케이스 상세 화면에서
 * 활성 커스텀 필드를 표준 필드 뒤에 렌더링한다. 설정된 커스텀 필드가 없으면 섹션 자체를 숨긴다.
 * 배치 순서/그룹 세밀 제어는 Phase 21(폼 레이아웃)의 몫이라 단순히 displayOrder로만 나열한다.
 */
export function CustomFieldsPanel({
  projectId,
  targetType,
  targetId,
}: {
  projectId: number;
  targetType: CustomFieldTargetPathSegment;
  targetId: number;
}) {
  const queryClient = useQueryClient();
  const queryKey = ['custom-field-values', projectId, targetType, targetId];

  const valuesQuery = useQuery({
    queryKey,
    queryFn: () => listCustomFieldValues(projectId, targetType, targetId),
    enabled: Number.isFinite(projectId) && Number.isFinite(targetId),
  });

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<Record<number, string>>({});

  function startEdit() {
    const next: Record<number, string> = {};
    (valuesQuery.data ?? []).forEach((v) => {
      next[v.fieldId] = toDraftString(v);
    });
    setDraft(next);
    setEditing(true);
  }

  const saveMutation = useMutation({
    mutationFn: () =>
      saveCustomFieldValues(
        projectId,
        targetType,
        targetId,
        (valuesQuery.data ?? [])
          .filter((v) => v.status === 'ACTIVE')
          .map((v) => ({ fieldId: v.fieldId, value: toSaveValue(v, draft[v.fieldId] ?? '') })),
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setEditing(false);
    },
  });

  if (valuesQuery.isLoading || !valuesQuery.data || valuesQuery.data.length === 0) {
    return null;
  }

  const values = valuesQuery.data;

  return (
    <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-slate-700">커스텀 필드</h2>
        {!editing && (
          <button type="button" onClick={startEdit} className="text-sm text-slate-500 hover:underline">
            편집
          </button>
        )}
      </div>

      {editing ? (
        <div>
          {values.map((v) => (
            <div key={v.fieldId} className="mb-3">
              <label className="mb-1 block text-sm text-slate-600">
                {v.label}
                {v.required && <span className="ml-0.5 text-red-500">*</span>}
                {v.status === 'DEPRECATED' && <span className="ml-1 text-xs text-slate-400">(비활성화됨, 읽기 전용)</span>}
              </label>
              {v.status === 'DEPRECATED' ? (
                <p className="rounded-md bg-slate-50 px-3 py-2 text-sm text-slate-500">{formatDisplayValue(v)}</p>
              ) : (
                <CustomFieldInput
                  value={v}
                  draftValue={draft[v.fieldId] ?? ''}
                  onChange={(next) => setDraft((prev) => ({ ...prev, [v.fieldId]: next }))}
                />
              )}
            </div>
          ))}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
            >
              저장
            </button>
            <button type="button" onClick={() => setEditing(false)} className="rounded-md border border-slate-300 px-4 py-2 text-sm">
              취소
            </button>
          </div>
          {saveMutation.isError && (
            <p className="mt-2 text-sm text-red-600">저장에 실패했습니다. 입력값을 확인해주세요.</p>
          )}
        </div>
      ) : (
        <dl className="grid grid-cols-2 gap-3 text-sm">
          {values.map((v) => (
            <div key={v.fieldId}>
              <dt className="text-slate-400">
                {v.label}
                {v.status === 'DEPRECATED' && <span className="ml-1 text-xs">(비활성화됨)</span>}
              </dt>
              <dd className="whitespace-pre-wrap text-slate-700">{formatDisplayValue(v)}</dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  );
}

function CustomFieldInput({
  value,
  draftValue,
  onChange,
}: {
  value: CustomFieldValue;
  draftValue: string;
  onChange: (next: string) => void;
}) {
  switch (value.dataType) {
    case 'NUMBER':
      return (
        <input
          type="number"
          value={draftValue}
          onChange={(e) => onChange(e.target.value)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      );
    case 'DATE':
      return (
        <input
          type="date"
          value={draftValue}
          onChange={(e) => onChange(e.target.value)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      );
    case 'BOOLEAN':
      return (
        <select
          value={draftValue}
          onChange={(e) => onChange(e.target.value)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">선택 안 함</option>
          <option value="true">예</option>
          <option value="false">아니오</option>
        </select>
      );
    case 'MULTI_SELECT':
      return (
        <div>
          <input
            type="text"
            value={draftValue}
            onChange={(e) => onChange(e.target.value)}
            placeholder="값을 쉼표(,)로 구분해 입력하세요"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <p className="mt-1 text-xs text-slate-400">열거형 옵션 선택 UI는 추후(Phase 22) 지원됩니다. 지금은 쉼표로 구분한 값을 직접 입력합니다.</p>
        </div>
      );
    case 'SINGLE_SELECT':
      return (
        <div>
          <input
            type="text"
            value={draftValue}
            onChange={(e) => onChange(e.target.value)}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <p className="mt-1 text-xs text-slate-400">열거형 옵션 선택 UI는 추후(Phase 22) 지원됩니다. 지금은 값을 직접 입력합니다.</p>
        </div>
      );
    case 'TEXT':
    default:
      return (
        <input
          type="text"
          value={draftValue}
          onChange={(e) => onChange(e.target.value)}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
      );
  }
}

function toDraftString(v: CustomFieldValue): string {
  if (v.value == null) return '';
  if (v.dataType === 'MULTI_SELECT') {
    try {
      const parsed = JSON.parse(v.value);
      return Array.isArray(parsed) ? parsed.join(', ') : v.value;
    } catch {
      return v.value;
    }
  }
  return v.value;
}

function toSaveValue(v: CustomFieldValue, draftValue: string): string | null {
  if (draftValue === '') return null;
  if (v.dataType === 'MULTI_SELECT') {
    const items = draftValue.split(',').map((s) => s.trim()).filter((s) => s.length > 0);
    return JSON.stringify(items);
  }
  return draftValue;
}

function formatDisplayValue(v: CustomFieldValue): string {
  if (v.value == null || v.value === '') return '-';
  if (v.dataType === 'MULTI_SELECT') {
    try {
      const parsed = JSON.parse(v.value);
      return Array.isArray(parsed) ? parsed.join(', ') : v.value;
    } catch {
      return v.value;
    }
  }
  if (v.dataType === 'BOOLEAN') {
    return v.value === 'true' ? '예' : v.value === 'false' ? '아니오' : v.value;
  }
  return v.value;
}
