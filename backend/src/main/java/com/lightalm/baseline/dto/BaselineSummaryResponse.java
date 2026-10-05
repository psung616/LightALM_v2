package com.lightalm.baseline.dto;

import com.lightalm.baseline.domain.Baseline;
import java.time.LocalDateTime;

/** 04-api.md §4.18 베이스라인 목록 원소. */
public record BaselineSummaryResponse(
        Long id,
        String name,
        String description,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        long itemCount) {

    public static BaselineSummaryResponse from(Baseline baseline, long itemCount) {
        return new BaselineSummaryResponse(
                baseline.getId(),
                baseline.getName(),
                baseline.getDescription(),
                baseline.getCreatedBy() != null ? baseline.getCreatedBy().getId() : null,
                baseline.getCreatedBy() != null ? baseline.getCreatedBy().getFullName() : null,
                baseline.getCreatedAt(),
                itemCount);
    }
}
