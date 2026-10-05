package com.lightalm.baseline.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.lightalm.domain.TargetType;
import java.time.LocalDateTime;

/** 베이스라인 상세의 포함 항목 1건. {@code snapshot}은 생성 시점에 얼린 필드 값(JSON 객체) 그대로다. */
public record BaselineItemResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        JsonNode snapshot,
        LocalDateTime capturedAt) {
}
