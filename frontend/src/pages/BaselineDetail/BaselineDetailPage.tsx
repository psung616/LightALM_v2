import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getBaseline, getBaselineDiff } from '../../api/baseline';
import { FullScreenLoader } from '../../components/FullScreenLoader';
import { StatusBadge } from '../../components/Badge';
import { BASELINE_TARGET_LABEL } from '../../types/baseline';
import type { BaselineFieldChange, BaselineItemChangeType, SnapshotValue } from '../../types/baseline';

// ADR-008(Phase 16) / 05-frontend.md §5.12. 베이스라인 상세(스냅샷 목록) + "현재와 비교"(diff).
// 필드 변경분은 추가됨(강조)/삭제됨(취소선)/값 변경됨(취소선 이전값 → 강조 현재값)으로 색상 구분한다.

const CHANGE_TYPE_LABEL: Record<BaselineItemChangeType, string> = {
  UNCHANGED: '변경 없음',
  MODIFIED: '변경됨',
  DELETED: '대상 삭제됨',
};

const CHANGE_TYPE_COLOR: Record<BaselineItemChangeType, string> = {
  UNCHANGED: 'bg-slate-100 text-slate-500',
  MODIFIED: 'bg-amber-100 text-amber-700',
  DELETED: 'bg-red-100 text-red-700',
};

function formatValue(value: SnapshotValue): string {
  if (value === null || value === undefined) {
    return '(없음)';
  }
  return String(value);
}

function FieldChangeRow({ change }: { change: BaselineFieldChange }) {
  return (
    <tr className="border-t border-slate-100 align-top">
      <td className="px-3 py-1.5 font-mono text-xs text-slate-500">{change.field}</td>
      <td className="px-3 py-1.5 text-xs">
        {change.changeKind === 'ADDED' && <span className="text-slate-400">추가됨</span>}
        {change.changeKind === 'REMOVED' && <span className="text-red-600">삭제됨</span>}
        {change.changeKind === 'MODIFIED' && <span className="text-amber-700">값 변경됨</span>}
      </td>
      <td className="px-3 py-1.5 text-sm">
        {change.before !== null && (
          <span className="whitespace-pre-wrap bg-red-50 text-red-700 line-through">{formatValue(change.before)}</span>
        )}
      </td>
      <td className="px-3 py-1.5 text-sm">
        {change.after !== null && (
          <span className="whitespace-pre-wrap bg-green-50 font-medium text-green-700">{formatValue(change.after)}</span>
        )}
      </td>
    </tr>
  );
}

export function BaselineDetailPage() {
  const { projectId, baselineId } = useParams<{ projectId: string; baselineId: string }>();
  const id = Number(projectId);
  const bid = Number(baselineId);
  const [compare, setCompare] = useState(false);

  const baselineQuery = useQuery({
    queryKey: ['baseline', id, bid],
    queryFn: () => getBaseline(id, bid),
  });

  // diff는 서버에 저장되지 않고 호출 시점에 계산되므로 캐시를 오래 쓰지 않는다.
  const diffQuery = useQuery({
    queryKey: ['baseline', id, bid, 'diff'],
    queryFn: () => getBaselineDiff(id, bid),
    enabled: compare,
    staleTime: 0,
  });

  if (baselineQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (baselineQuery.isError || !baselineQuery.data) {
    return <p className="text-slate-600">베이스라인을 불러올 수 없습니다.</p>;
  }
  const baseline = baselineQuery.data;
  const diff = diffQuery.data;

  return (
    <div>
      <Link to={`/projects/${id}/baselines`} className="text-sm text-slate-500 hover:text-slate-800">
        ← 베이스라인 목록
      </Link>
      <div className="mb-4 mt-2 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">{baseline.name}</h1>
          <p className="text-sm text-slate-500">
            {baseline.createdByName ?? '알 수 없음'} · {new Date(baseline.createdAt).toLocaleString('ko-KR')} · 항목{' '}
            {baseline.items.length}개
          </p>
          {baseline.description && <p className="mt-1 whitespace-pre-wrap text-sm text-slate-600">{baseline.description}</p>}
        </div>
        <button
          type="button"
          onClick={() => {
            if (compare) {
              void diffQuery.refetch();
            } else {
              setCompare(true);
            }
          }}
          className="rounded-md bg-primary px-3 py-1.5 text-sm text-white hover:bg-primary-hover"
        >
          {compare ? '다시 비교' : '현재와 비교'}
        </button>
      </div>

      {compare && (
        <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="mb-2 text-sm font-semibold text-slate-700">현재 값과 비교</h2>
          {diffQuery.isLoading && <p className="text-sm text-slate-400">비교 중...</p>}
          {diffQuery.isError && <p className="text-sm text-red-600">비교 결과를 불러올 수 없습니다.</p>}
          {diff && (
            <>
              <p className="mb-3 text-xs text-slate-500">
                변경 없음 {diff.unchangedCount} · 변경됨 {diff.modifiedCount} · 대상 삭제됨 {diff.deletedCount} (비교 시각{' '}
                {new Date(diff.comparedAt).toLocaleString('ko-KR')})
              </p>
              <ul className="flex flex-col gap-3">
                {diff.items.map((item) => (
                  <li key={`${item.targetType}:${item.targetId}`} className="rounded-md border border-slate-200 p-3">
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-400">{BASELINE_TARGET_LABEL[item.targetType]}</span>
                      <span className="font-mono text-xs text-slate-500">{item.key ?? `#${item.targetId}`}</span>
                      <span className={`text-sm ${item.changeType === 'DELETED' ? 'text-slate-400 line-through' : 'text-slate-800'}`}>
                        {item.title ?? ''}
                      </span>
                      <span className={`ml-auto rounded-full px-2 py-0.5 text-xs font-medium ${CHANGE_TYPE_COLOR[item.changeType]}`}>
                        {CHANGE_TYPE_LABEL[item.changeType]}
                      </span>
                    </div>
                    {item.changes.length > 0 && (
                      <table className="mt-2 w-full">
                        <thead className="text-left text-xs text-slate-400">
                          <tr>
                            <th className="px-3 py-1 font-normal">필드</th>
                            <th className="px-3 py-1 font-normal">구분</th>
                            <th className="px-3 py-1 font-normal">베이스라인 값</th>
                            <th className="px-3 py-1 font-normal">현재 값</th>
                          </tr>
                        </thead>
                        <tbody>
                          {item.changes.map((change) => (
                            <FieldChangeRow key={change.field} change={change} />
                          ))}
                        </tbody>
                      </table>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <h2 className="border-b border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700">포함 항목 (생성 시점 스냅샷)</h2>
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-2">유형</th>
              <th className="px-4 py-2">키</th>
              <th className="px-4 py-2">제목</th>
              <th className="px-4 py-2">상태</th>
              <th className="px-4 py-2">우선순위</th>
            </tr>
          </thead>
          <tbody>
            {baseline.items.map((item) => (
              <tr key={item.id} className="border-t border-slate-100">
                <td className="px-4 py-2 text-slate-500">{BASELINE_TARGET_LABEL[item.targetType]}</td>
                <td className="px-4 py-2 font-mono text-xs text-slate-500">{formatValue(item.snapshot.key ?? null)}</td>
                <td className="px-4 py-2 text-slate-800">{formatValue(item.snapshot.title ?? null)}</td>
                <td className="px-4 py-2">
                  {typeof item.snapshot.status === 'string' ? <StatusBadge status={item.snapshot.status} /> : '-'}
                </td>
                <td className="px-4 py-2 text-slate-600">{formatValue(item.snapshot.priority ?? null)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
