import { useRef, useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { getCurrentLicense, listLicenseHistory, uploadLicense } from '../../api/license';
import type { ApiErrorResponse } from '../../types/common';

const LICENSE_TYPE_LABELS: Record<string, string> = {
  TRIAL: '체험판',
  STANDARD: '표준',
  ENTERPRISE: '엔터프라이즈',
};

const LICENSE_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '활성',
  SUPERSEDED: '교체됨',
  REVOKED: '폐기됨',
};

function formatDDay(expiresAt: string | null): string {
  if (!expiresAt) {
    return '무기한';
  }
  const diffMs = new Date(expiresAt).getTime() - Date.now();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays < 0) {
    return `만료됨 (D+${Math.abs(diffDays)})`;
  }
  return `D-${diffDays}`;
}

export function AdminLicensesPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const currentQuery = useQuery({ queryKey: ['licenses', 'current'], queryFn: getCurrentLicense, retry: false });
  const historyQuery = useQuery({ queryKey: ['licenses', 'history'], queryFn: () => listLicenseHistory(0, 50) });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadLicense(file),
    onSuccess: () => {
      setUploadError(null);
      queryClient.invalidateQueries({ queryKey: ['licenses'] });
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    onError: (err) => {
      if (axios.isAxiosError<ApiErrorResponse>(err) && err.response?.data?.message) {
        setUploadError(err.response.data.message);
      } else {
        setUploadError('라이센스 업로드에 실패했습니다.');
      }
    },
  });

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      uploadMutation.mutate(file);
    }
  }

  const current = currentQuery.data;
  const seatUsageRatio = current ? Math.min(100, Math.round((current.seatsUsed / current.seatLimit) * 100)) : 0;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">라이센스 관리</h1>
        <div>
          <input ref={fileInputRef} type="file" accept=".json,application/json,text/plain" className="hidden" onChange={handleFileChange} />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isPending}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-hover disabled:opacity-50"
          >
            {uploadMutation.isPending ? '업로드 중...' : '라이센스 파일 업로드'}
          </button>
        </div>
      </div>

      {uploadError && <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-600">{uploadError}</p>}

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-500">현재 활성 라이센스</h2>
        {currentQuery.isLoading ? (
          <p className="text-sm text-slate-500">불러오는 중...</p>
        ) : !current ? (
          <p className="text-sm text-amber-600">활성화된 라이센스가 없습니다. 파일을 업로드해주세요.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <div className="text-slate-500">조직명</div>
              <div className="font-medium text-slate-900">{current.organizationName}</div>
            </div>
            <div>
              <div className="text-slate-500">라이센스 타입</div>
              <div className="font-medium text-slate-900">{LICENSE_TYPE_LABELS[current.licenseType] ?? current.licenseType}</div>
            </div>
            <div>
              <div className="text-slate-500">시트 사용량</div>
              <div className="font-medium text-slate-900">
                {current.seatsUsed} / {current.seatLimit}
              </div>
              <div className="mt-1 h-2 w-full rounded-full bg-slate-100">
                <div className="h-2 rounded-full bg-primary" style={{ width: `${seatUsageRatio}%` }} />
              </div>
            </div>
            <div>
              <div className="text-slate-500">만료일</div>
              <div className={`font-medium ${!current.expiresAt ? 'text-slate-900' : new Date(current.expiresAt) < new Date() ? 'text-red-600' : 'text-slate-900'}`}>
                {current.expiresAt ?? '무기한'} ({formatDDay(current.expiresAt)})
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-3 py-2">파일명</th>
              <th className="px-3 py-2">업로드자</th>
              <th className="px-3 py-2">업로드일시</th>
              <th className="px-3 py-2">상태</th>
            </tr>
          </thead>
          <tbody>
            {historyQuery.data?.content.map((l) => (
              <tr key={l.id} className="border-b border-slate-100 last:border-0">
                <td className="px-3 py-2">{l.rawFileName}</td>
                <td className="px-3 py-2">{l.uploadedByUsername ?? '-'}</td>
                <td className="px-3 py-2">{l.uploadedAt}</td>
                <td className="px-3 py-2">{LICENSE_STATUS_LABELS[l.status] ?? l.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
