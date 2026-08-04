import { apiClient } from './client';
import type { PageResponse, Priority, TestCaseStatus } from '../types/common';
import type { TestCase } from '../types/testCase';

export interface TestCaseFilters {
  requirementId?: number;
  status?: TestCaseStatus;
  priority?: Priority;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface CreateTestCaseRequest {
  title: string;
  description?: string;
  preconditions?: string;
  steps: string;
  expectedResult: string;
  priority?: Priority;
  requirementId?: number;
}

export interface UpdateTestCaseRequest extends CreateTestCaseRequest {
  status?: TestCaseStatus;
}

export async function listTestCases(projectId: number, filters: TestCaseFilters = {}): Promise<PageResponse<TestCase>> {
  const { data } = await apiClient.get<PageResponse<TestCase>>(`/projects/${projectId}/test-cases`, { params: filters });
  return data;
}

export async function getTestCase(projectId: number, tcId: number): Promise<TestCase> {
  const { data } = await apiClient.get<TestCase>(`/projects/${projectId}/test-cases/${tcId}`);
  return data;
}

export async function createTestCase(projectId: number, request: CreateTestCaseRequest): Promise<TestCase> {
  const { data } = await apiClient.post<TestCase>(`/projects/${projectId}/test-cases`, request);
  return data;
}

export async function updateTestCase(projectId: number, tcId: number, request: UpdateTestCaseRequest): Promise<TestCase> {
  const { data } = await apiClient.put<TestCase>(`/projects/${projectId}/test-cases/${tcId}`, request);
  return data;
}

export async function deleteTestCase(projectId: number, tcId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/test-cases/${tcId}`);
}

export async function listTestCasesForRequirement(projectId: number, reqId: number): Promise<TestCase[]> {
  const { data } = await apiClient.get<TestCase[]>(`/projects/${projectId}/requirements/${reqId}/test-cases`);
  return data;
}
