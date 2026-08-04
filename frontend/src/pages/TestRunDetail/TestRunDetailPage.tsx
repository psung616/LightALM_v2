import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { addCasesToRun, changeTestRunStatus, getTestRun, recordTestResult } from '../../api/testRun';
import { listTestCases } from '../../api/testCase';
import type { TestResult, TestRunStatus } from '../../types/common';
import { StatusBadge } from '../../components/Badge';
import { FullScreenLoader } from '../../components/FullScreenLoader';

const STATUS_OPTIONS: TestRunStatus[] = ['PLANNED', 'IN_PROGRESS', 'COMPLETED'];
const RESULT_OPTIONS: TestResult[] = ['PASS', 'FAIL', 'BLOCKED', 'SKIPPED'];

export function TestRunDetailPage() {
  const { projectId, runId } = useParams<{ projectId: string; runId: string }>();
  const id = Number(projectId);
  const rid = Number(runId);
  const queryClient = useQueryClient();

  const [selectedCaseIds, setSelectedCaseIds] = useState<number[]>([]);
  const [actualResults, setActualResults] = useState<Record<number, string>>({});

  const runQuery = useQuery({
    queryKey: ['test-run', rid],
    queryFn: () => getTestRun(id, rid),
    enabled: Number.isFinite(id) && Number.isFinite(rid),
  });

  const testCasesQuery = useQuery({
    queryKey: ['project', id, 'test-cases', 'all'],
    queryFn: () => listTestCases(id, { size: 200 }),
    enabled: Number.isFinite(id),
  });

  useEffect(() => {
    if (!runQuery.data) return;
    setActualResults((prev) => {
      const next = { ...prev };
      for (const r of runQuery.data!.results) {
        if (next[r.testCaseId] === undefined) {
          next[r.testCaseId] = r.actualResult ?? '';
        }
      }
      return next;
    });
  }, [runQuery.data]);

  const statusMutation = useMutation({
    mutationFn: (status: TestRunStatus) => changeTestRunStatus(id, rid, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['test-run', rid] }),
  });

  const addCasesMutation = useMutation({
    mutationFn: () => addCasesToRun(id, rid, selectedCaseIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-run', rid] });
      setSelectedCaseIds([]);
    },
  });

  const recordMutation = useMutation({
    mutationFn: ({ testCaseId, result, actualResult }: { testCaseId: number; result: TestResult; actualResult: string }) =>
      recordTestResult(id, rid, testCaseId, result, actualResult || undefined),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['test-run', rid] }),
  });

  if (runQuery.isLoading) {
    return <FullScreenLoader />;
  }
  if (runQuery.isError || !runQuery.data) {
    return <p className="text-sm text-red-600">테스트런을 불러오지 못했습니다.</p>;
  }

  const run = runQuery.data;
  const includedIds = new Set(run.results.map((r) => r.testCaseId));
  const availableCases = (testCasesQuery.data?.content ?? []).filter((tc) => !includedIds.has(tc.id));

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">{run.name}</h1>
        <div className="flex items-center gap-2">
          <StatusBadge status={run.status} />
          <select
            value={run.status}
            onChange={(e) => statusMutation.mutate(e.target.value as TestRunStatus)}
            className="rounded-md border border-slate-300 px-2 py-1 text-sm"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">테스트케이스 추가</h2>
        {availableCases.length === 0 ? (
          <p className="text-sm text-slate-400">추가할 수 있는 테스트케이스가 없습니다.</p>
        ) : (
          <>
            <div className="mb-3 max-h-48 overflow-y-auto rounded-md border border-slate-200">
              {availableCases.map((tc) => (
                <label key={tc.id} className="flex items-center gap-2 border-b border-slate-100 px-3 py-1.5 text-sm last:border-0 hover:bg-slate-50">
                  <input
                    type="checkbox"
                    checked={selectedCaseIds.includes(tc.id)}
                    onChange={(e) =>
                      setSelectedCaseIds((prev) => (e.target.checked ? [...prev, tc.id] : prev.filter((v) => v !== tc.id)))
                    }
                  />
                  <span className="font-medium text-slate-700">{tc.tcKey}</span>
                  <span className="text-slate-500">{tc.title}</span>
                </label>
              ))}
            </div>
            <button
              type="button"
              disabled={selectedCaseIds.length === 0 || addCasesMutation.isPending}
              onClick={() => addCasesMutation.mutate()}
              className="rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800 disabled:opacity-50"
            >
              추가 ({selectedCaseIds.length})
            </button>
          </>
        )}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">실행 체크리스트</h2>
        <div className="flex flex-col gap-3">
          {run.results.map((r) => (
            <div key={r.testCaseId} className="rounded-md border border-slate-200 p-3">
              <div className="mb-2 flex items-center justify-between">
                <div className="text-sm">
                  <span className="mr-2 font-medium text-slate-700">{r.tcKey}</span>
                  {r.testCaseTitle}
                </div>
                <StatusBadge status={r.result} />
              </div>
              <div className="mb-2 flex gap-1.5">
                {RESULT_OPTIONS.map((opt) => (
                  <button
                    key={opt}
                    type="button"
                    disabled={recordMutation.isPending}
                    onClick={() =>
                      recordMutation.mutate({ testCaseId: r.testCaseId, result: opt, actualResult: actualResults[r.testCaseId] ?? '' })
                    }
                    className={`rounded-md px-2.5 py-1 text-xs font-medium ${
                      r.result === opt ? 'bg-slate-900 text-white' : 'border border-slate-300 text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    {opt}
                  </button>
                ))}
              </div>
              <input
                type="text"
                value={actualResults[r.testCaseId] ?? ''}
                onChange={(e) => setActualResults((prev) => ({ ...prev, [r.testCaseId]: e.target.value }))}
                onBlur={() => recordMutation.mutate({ testCaseId: r.testCaseId, result: r.result, actualResult: actualResults[r.testCaseId] ?? '' })}
                placeholder="실제 결과 메모"
                className="w-full rounded-md border border-slate-300 px-2 py-1 text-sm"
              />
            </div>
          ))}
          {run.results.length === 0 && <p className="text-sm text-slate-400">이 테스트런에 포함된 테스트케이스가 없습니다.</p>}
        </div>
      </div>
    </div>
  );
}
