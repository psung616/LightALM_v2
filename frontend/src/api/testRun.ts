import { apiClient } from './client';
import type { PageResponse, TestResult, TestRunStatus } from '../types/common';
import type { TestRun, TestRunResult } from '../types/testCase';

export interface TestRunFilters {
  page?: number;
  size?: number;
}

export async function listTestRuns(projectId: number, filters: TestRunFilters = {}): Promise<PageResponse<TestRun>> {
  const { data } = await apiClient.get<PageResponse<TestRun>>(`/projects/${projectId}/test-runs`, { params: filters });
  return data;
}

export async function getTestRun(projectId: number, runId: number): Promise<TestRun> {
  const { data } = await apiClient.get<TestRun>(`/projects/${projectId}/test-runs/${runId}`);
  return data;
}

export async function createTestRun(projectId: number, name: string, releaseId?: number): Promise<TestRun> {
  const { data } = await apiClient.post<TestRun>(`/projects/${projectId}/test-runs`, { name, releaseId });
  return data;
}

export async function addCasesToRun(projectId: number, runId: number, testCaseIds: number[]): Promise<TestRun> {
  const { data } = await apiClient.post<TestRun>(`/projects/${projectId}/test-runs/${runId}/cases`, { testCaseIds });
  return data;
}

export async function recordTestResult(
  projectId: number,
  runId: number,
  testCaseId: number,
  result: TestResult,
  actualResult?: string,
): Promise<TestRunResult> {
  const { data } = await apiClient.patch<TestRunResult>(`/projects/${projectId}/test-runs/${runId}/results/${testCaseId}`, {
    result,
    actualResult,
  });
  return data;
}

export async function changeTestRunStatus(projectId: number, runId: number, status: TestRunStatus): Promise<TestRun> {
  const { data } = await apiClient.patch<TestRun>(`/projects/${projectId}/test-runs/${runId}/status`, { status });
  return data;
}
