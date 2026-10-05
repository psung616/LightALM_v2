import { apiClient } from './client';
import type { FormLayoutResponse, FormLayoutTargetType, SaveFormLayoutRequest } from '../types/formLayout';

export async function getFormLayoutForConfig(
  projectId: number,
  targetType: FormLayoutTargetType,
): Promise<FormLayoutResponse> {
  const { data } = await apiClient.get<FormLayoutResponse>(`/projects/${projectId}/config/form-layouts/${targetType}`);
  return data;
}

export async function saveFormLayout(
  projectId: number,
  targetType: FormLayoutTargetType,
  request: SaveFormLayoutRequest,
): Promise<FormLayoutResponse> {
  const { data } = await apiClient.put<FormLayoutResponse>(
    `/projects/${projectId}/config/form-layouts/${targetType}`,
    request,
  );
  return data;
}

export async function getFormLayoutForRender(
  projectId: number,
  targetType: FormLayoutTargetType,
): Promise<FormLayoutResponse> {
  const { data } = await apiClient.get<FormLayoutResponse>(`/projects/${projectId}/form-layouts/${targetType}`);
  return data;
}
