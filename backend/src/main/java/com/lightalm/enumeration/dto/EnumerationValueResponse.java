package com.lightalm.enumeration.dto;

import com.lightalm.enumeration.domain.EnumerationValueStatus;
import com.lightalm.enumeration.domain.ProjectEnumerationValue;

/**
 * 열거형 값 응답. 프로젝트가 아직 집합을 만들지 않은 경우(기본 Priority enum 미러링)에는
 * {@code id}가 null인 채로 내려간다 — {@code EnumerationSetService.defaultActiveValues}.
 */
public record EnumerationValueResponse(
        Long id,
        String valueKey,
        String label,
        Integer displayOrder,
        Boolean isSystemDefault,
        EnumerationValueStatus status
) {

    public static EnumerationValueResponse from(ProjectEnumerationValue value) {
        return new EnumerationValueResponse(
                value.getId(),
                value.getValueKey(),
                value.getLabel(),
                value.getDisplayOrder(),
                value.getIsSystemDefault(),
                value.getStatus());
    }
}
