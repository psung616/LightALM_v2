// ADR-012 §A(Phase 20) 커스텀 필드. 04-api.md §4.24 참고.

/** config/custom-fields, custom-fields의 쿼리파라미터 targetType — 백엔드 TargetType enum 그대로. */
export type CustomFieldTargetType = 'REQUIREMENT' | 'ISSUE' | 'TEST_CASE';

/** .../{targetType}/{targetId}/custom-field-values 경로 세그먼트. */
export type CustomFieldTargetPathSegment = 'requirements' | 'issues' | 'test-cases';

export type CustomFieldDataType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'SINGLE_SELECT' | 'MULTI_SELECT';
export type CustomFieldStatus = 'ACTIVE' | 'DEPRECATED';

export interface CustomFieldDefinitionDetail {
  id: number;
  targetType: CustomFieldTargetType;
  fieldKey: string;
  label: string;
  dataType: CustomFieldDataType;
  enumerationSetId: number | null;
  required: boolean;
  defaultValue: string | null;
  displayOrder: number;
  status: CustomFieldStatus;
  createdByUsername: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CustomFieldDefinitionSummary {
  id: number;
  targetType: CustomFieldTargetType;
  fieldKey: string;
  label: string;
  dataType: CustomFieldDataType;
  enumerationSetId: number | null;
  required: boolean;
  defaultValue: string | null;
  displayOrder: number;
}

export interface CustomFieldValue {
  fieldId: number;
  fieldKey: string;
  label: string;
  dataType: CustomFieldDataType;
  required: boolean;
  status: CustomFieldStatus;
  value: string | null;
}
