package com.lightalm.baseline.dto;

import com.lightalm.baseline.domain.Baseline;
import java.time.LocalDateTime;
import java.util.List;

/** 04-api.md §4.18 {@code GET .../baselines/{baselineId}}(및 POST 생성 응답) — 포함 항목 스냅샷 목록 포함. */
public record BaselineDetailResponse(
        Long id,
        String name,
        String description,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        List<BaselineItemResponse> items) {

    public static BaselineDetailResponse of(Baseline baseline, List<BaselineItemResponse> items) {
        return new BaselineDetailResponse(
                baseline.getId(),
                baseline.getName(),
                baseline.getDescription(),
                baseline.getCreatedBy() != null ? baseline.getCreatedBy().getId() : null,
                baseline.getCreatedBy() != null ? baseline.getCreatedBy().getFullName() : null,
                baseline.getCreatedAt(),
                items);
    }
}
