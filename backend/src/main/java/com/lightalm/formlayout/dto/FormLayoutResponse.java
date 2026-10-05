package com.lightalm.formlayout.dto;

import com.lightalm.domain.TargetType;
import java.util.List;

/**
 * ADR-012 §B.3. {@code id == null}이면 프로젝트가 이 target_type에 대해 레이아웃을 한 번도
 * 설정하지 않았다는 뜻이며(아직 DB에 form_layouts 행이 없음), 이 경우 이 응답은
 * {@link com.lightalm.formlayout.service.StandardFieldKeyRegistry}의 기본 순서로 합성한
 * "기본 레이아웃"이다(§B.3 하위호환 규칙).
 */
public record FormLayoutResponse(
        Long id,
        TargetType targetType,
        List<FormLayoutSectionResponse> sections
) {
}
