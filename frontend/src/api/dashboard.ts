import { apiClient } from './client';
import type { MyDashboard, ProjectDashboardSummary } from '../types/dashboard';

export async function getProjectDashboardSummary(projectId: number): Promise<ProjectDashboardSummary> {
  const { data } = await apiClient.get<ProjectDashboardSummary>(`/projects/${projectId}/dashboard/summary`);
  return data;
}

export async function getMyDashboard(): Promise<MyDashboard> {
  const { data } = await apiClient.get<MyDashboard>('/me/dashboard');
  return data;
}
