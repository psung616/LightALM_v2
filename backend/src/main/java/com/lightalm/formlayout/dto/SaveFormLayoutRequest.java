package com.lightalm.formlayout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** ADR-012 §B.3 PUT 요청 바디 — 레이아웃 전체 치환(섹션/필드 트리 일괄 저장). */
public record SaveFormLayoutRequest(
        @NotNull(message = "sections는 필수입니다.") @Valid List<SaveFormLayoutSectionRequest> sections
) {
}
