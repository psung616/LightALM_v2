package com.lightalm.baseline.dto;

import com.lightalm.domain.TargetType;
import jakarta.validation.constraints.NotNull;

/** 04-api.md §4.18 베이스라인 생성 요청의 {@code itemRefs[]} 원소. */
public record BaselineItemRef(
        @NotNull(message = "targetType은 필수입니다.") TargetType targetType,
        @NotNull(message = "targetId는 필수입니다.") Long targetId) {
}
