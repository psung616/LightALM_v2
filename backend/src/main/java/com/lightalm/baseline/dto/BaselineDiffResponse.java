package com.lightalm.baseline.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 04-api.md §4.18 {@code GET .../baselines/{baselineId}/diff}. 저장하지 않고 조회 시점에 계산한 결과다.
 */
public record BaselineDiffResponse(
        Long baselineId,
        String baselineName,
        LocalDateTime baselineCreatedAt,
        LocalDateTime comparedAt,
        int unchangedCount,
        int modifiedCount,
        int deletedCount,
        List<BaselineItemDiff> items) {
}
