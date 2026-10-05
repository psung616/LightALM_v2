import type { ReactNode } from 'react';
import type { FormLayoutResponse } from '../types/formLayout';

/**
 * ADR-012 §B.4/§B.3 하위호환 규칙을 프론트엔드에서 구현한다: 레이아웃이 설정되지 않은
 * 프로젝트+target_type({@code layout.id == null})은 {@code fallback}(기존 고정 폼 마크업)을
 * 그대로 렌더링해 회귀가 전혀 없게 한다. 레이아웃이 실제로 설정된 경우에만 섹션/순서/노출
 * 여부를 반영해 동적으로 렌더링한다.
 *
 * CUSTOM 필드 배치는 이 컴포넌트에서 렌더링하지 않는다 — 요구사항/이슈/테스트케이스의
 * 커스텀 필드 입력은 대상(target_id)이 있어야 하는 {@code CustomFieldsPanel}(상세 화면 전용,
 * ADR-012 §A)이 계속 담당한다(생성 폼에는 대상이 아직 없어 커스텀 필드 값을 저장할 수 없음).
 * 설정 화면에서 CUSTOM 필드를 레이아웃에 배치해도, 생성/수정 폼에서는 표준 필드만 이 컴포넌트가
 * 그려주고 커스텀 필드는 기존처럼 별도 패널에서 다룬다 — 알려진 범위 제한(구현 시 기록).
 */
export function DynamicStandardFieldsLayout({
  layout,
  fieldRenderers,
  fallback,
}: {
  layout: FormLayoutResponse | undefined;
  fieldRenderers: Record<string, () => ReactNode>;
  fallback: ReactNode;
}) {
  if (!layout || layout.id == null) {
    return <>{fallback}</>;
  }

  return (
    <>
      {layout.sections.map((section) => {
        const visibleFields = section.fields.filter(
          (f) => f.fieldSource === 'STANDARD' && f.visible && f.standardFieldKey && fieldRenderers[f.standardFieldKey],
        );
        if (visibleFields.length === 0) return null;
        return (
          <div key={section.id ?? section.displayOrder} className="mb-4">
            {section.title && (
              <h3 className="mb-2 text-xs font-semibold uppercase text-slate-400">{section.title}</h3>
            )}
            <div className="flex flex-col gap-3">
              {visibleFields.map((f) => (
                <div key={f.id}>{fieldRenderers[f.standardFieldKey as string]()}</div>
              ))}
            </div>
          </div>
        );
      })}
    </>
  );
}
