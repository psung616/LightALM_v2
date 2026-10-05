import { apiClient } from './client';
import type {
  CustomFieldDataType,
  CustomFieldDefinitionDetail,
  CustomFieldDefinitionSummary,
  CustomFieldTargetPathSegment,
  CustomFieldTargetType,
  CustomFieldValue,
} from '../types/customField';

export interface CreateCustomFieldDefinitionRequest {
  targetType: CustomFieldTargetType;
  fieldKey: string;
  label: string;
  dataType: CustomFieldDataType;
  enumerationSetId?: number;
  required?: boolean;
  defaultValue?: string;
}

export interface UpdateCustomFieldDefinitionRequest {
  label: string;
  required?: boolean;
  defaultValue?: string;
  displayOrder?: number;
}

export async function listCustomFieldDefinitionsForConfig(
  projectId: number,
  targetType: CustomFieldTargetType,
): Promise<CustomFieldDefinitionDetail[]> {
  const { data } = await apiClient.get<CustomFieldDefinitionDetail[]>(`/projects/${projectId}/config/custom-fields`, {
    params: { targetType },
  });
  return data;
}

export async function createCustomFieldDefinition(
  projectId: number,
  request: CreateCustomFieldDefinitionRequest,
): Promise<CustomFieldDefinitionDetail> {
  const { data } = await apiClient.post<CustomFieldDefinitionDetail>(`/projects/${projectId}/config/custom-fields`, request);
  return data;
}

export async function updateCustomFieldDefinition(
  projectId: number,
  fieldId: number,
  request: UpdateCustomFieldDefinitionRequest,
): Promise<CustomFieldDefinitionDetail> {
  const { data } = await apiClient.put<CustomFieldDefinitionDetail>(
    `/projects/${projectId}/config/custom-fields/${fieldId}`,
    request,
  );
  return data;
}

export async function deprecateCustomFieldDefinition(projectId: number, fieldId: number): Promise<void> {
  await apiClient.delete(`/projects/${projectId}/config/custom-fields/${fieldId}`);
}

export async function listActiveCustomFieldDefinitions(
  projectId: number,
  targetType: CustomFieldTargetType,
): Promise<CustomFieldDefinitionSummary[]> {
  const { data } = await apiClient.get<CustomFieldDefinitionSummary[]>(`/projects/${projectId}/custom-fields`, {
    params: { targetType },
  });
  return data;
}

export async function listCustomFieldValues(
  projectId: number,
  targetType: CustomFieldTargetPathSegment,
  targetId: number,
): Promise<CustomFieldValue[]> {
  const { data } = await apiClient.get<CustomFieldValue[]>(
    `/projects/${projectId}/${targetType}/${targetId}/custom-field-values`,
  );
  return data;
}

export async function saveCustomFieldValues(
  projectId: number,
  targetType: CustomFieldTargetPathSegment,
  targetId: number,
  values: { fieldId: number; value: string | null }[],
): Promise<CustomFieldValue[]> {
  const { data } = await apiClient.put<CustomFieldValue[]>(
    `/projects/${projectId}/${targetType}/${targetId}/custom-field-values`,
    { values },
  );
  return data;
}
