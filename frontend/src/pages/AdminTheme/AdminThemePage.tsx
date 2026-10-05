import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { getThemeSettings, updateThemeSettings } from '../../api/theme';
import { useTheme } from '../../theme/ThemeContext';
import type { ApiErrorResponse } from '../../types/common';
import type { ThemeColorPreset } from '../../types/theme';

/**
 * ADR-014 §5.4/§5.23. 시스템 ADMIN 전용 — 5개 색상 프리셋 스와치 카드 중 하나를 선택해 저장한다.
 */
export function AdminThemePage() {
  const queryClient = useQueryClient();
  const { setColorPreset } = useTheme();
  const [selectedPreset, setSelectedPreset] = useState<ThemeColorPreset | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  const settingsQuery = useQuery({ queryKey: ['theme-settings'], queryFn: getThemeSettings });

  const updateMutation = useMutation({
    mutationFn: (preset: ThemeColorPreset) => updateThemeSettings(preset),
    onSuccess: (data) => {
      setSaveError(null);
      setSelectedPreset(null);
      setColorPreset(data.colorPreset);
      queryClient.setQueryData(['theme-settings'], data);
    },
    onError: (err) => {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response?.data?.message) {
        setSaveError(err.response.data.message);
      } else {
        setSaveError('테마 설정 저장에 실패했습니다.');
      }
    },
  });

  const current = settingsQuery.data;
  const activePreset = selectedPreset ?? current?.colorPreset ?? 'DEFAULT';

  function handleSave() {
    if (selectedPreset) {
      updateMutation.mutate(selectedPreset);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-semibold text-slate-900">테마 설정</h1>

      {saveError && <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{saveError}</p>}

      {settingsQuery.isLoading ? (
        <p className="text-sm text-slate-500">불러오는 중...</p>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-5 gap-3">
            {current?.availablePresets.map((option) => (
              <button
                key={option.code}
                type="button"
                onClick={() => setSelectedPreset(option.code)}
                className={`rounded-lg border p-3 text-left transition ${
                  activePreset === option.code
                    ? 'border-primary ring-2 ring-primary'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div
                  className="mb-2 h-10 w-full rounded-md"
                  style={{ backgroundColor: option.primaryColor }}
                />
                <div className="text-sm font-medium text-slate-900">{option.label}</div>
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleSave}
              disabled={!selectedPreset || updateMutation.isPending}
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
            >
              {updateMutation.isPending ? '저장 중...' : '저장'}
            </button>
            {current?.updatedByFullName && (
              <span className="text-sm text-slate-500">
                마지막 변경: {current.updatedByFullName}
                {current.updatedAt ? `, ${current.updatedAt}` : ''}
              </span>
            )}
          </div>
        </>
      )}
    </div>
  );
}
