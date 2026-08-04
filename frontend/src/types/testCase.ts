import type { Priority, TestCaseStatus, TestResult, TestRunStatus } from './common';

export interface TestCase {
  id: number;
  projectId: number;
  requirementId: number | null;
  requirementKey: string | null;
  tcKey: string;
  title: string;
  description: string | null;
  preconditions: string | null;
  steps: string;
  expectedResult: string;
  priority: Priority;
  status: TestCaseStatus;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TestRunResult {
  id: number;
  testCaseId: number;
  tcKey: string;
  testCaseTitle: string;
  result: TestResult;
  actualResult: string | null;
  executedById: number | null;
  executedByName: string | null;
  executedAt: string | null;
}

export interface TestRun {
  id: number;
  projectId: number;
  releaseId: number | null;
  releaseVersion: string | null;
  name: string;
  status: TestRunStatus;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  results: TestRunResult[];
}
