package com.lightalm.formlayout.dto;

import com.lightalm.formlayout.domain.FieldSource;

/**
 * STANDARD/CUSTOM 공통 응답. 설계 문서에는 없는 편의 필드({@code standardFieldLabel},
 * {@code customFieldLabel}, {@code customFieldDataType})를 추가했다 — 프론트엔드가 별도
 * 조회 없이 바로 렌더링할 수 있게 하기 위함(구현 시 추가한 편의 필드, ADR 본문에는 없음).
 */
public record FormLayoutFieldResponse(
        Long id,
        FieldSource fieldSource,
        String standardFieldKey,
        String standardFieldLabel,
        Long customFieldId,
        String customFieldLabel,
        String customFieldDataType,
        Integer displayOrder,
        Boolean visible
) {
}
