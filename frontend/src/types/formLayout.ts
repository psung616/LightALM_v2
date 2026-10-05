// ADR-012 §B(Phase 21) 폼 레이아웃. 04-api.md §4.25 참고.

/** config/form-layouts/{targetType}, form-layouts/{targetType}의 경로 세그먼트 — 백엔드 TargetType enum 그대로. */
export type FormLayoutTargetType = 'REQUIREMENT' | 'ISSUE' | 'TEST_CASE';

export type FieldSource = 'STANDARD' | 'CUSTOM';

export interface FormLayoutField {
  id: number | null;
  fieldSource: FieldSource;
  standardFieldKey: string | null;
  standardFieldLabel: string | null;
  customFieldId: number | null;
  customFieldLabel: string | null;
  customFieldDataType: string | null;
  displayOrder: number;
  visible: boolean;
}

export interface FormLayoutSection {
  id: number | null;
  title: string | null;
  displayOrder: number;
  fields: FormLayoutField[];
}

/**
 * {@code id === null}이면 이 프로젝트+targetType은 레이아웃을 설정한 적이 없고,
 * 백엔드가 합성한 "기본 레이아웃"(표준 필드 기본 순서, 섹션 없이 일렬로)이라는 뜻이다.
 */
export interface FormLayoutResponse {
  id: number | null;
  targetType: FormLayoutTargetType;
  sections: FormLayoutSection[];
}

export interface SaveFormLayoutFieldRequest {
  fieldSource: FieldSource;
  standardFieldKey?: string | null;
  customFieldId?: number | null;
  displayOrder: number;
  visible: boolean;
}

export interface SaveFormLayoutSectionRequest {
  title: string;
  displayOrder: number;
  fields: SaveFormLayoutFieldRequest[];
}

export interface SaveFormLayoutRequest {
  sections: SaveFormLayoutSectionRequest[];
}
