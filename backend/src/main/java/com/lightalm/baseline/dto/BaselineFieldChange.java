package com.lightalm.baseline.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 필드 1개의 변경분. {@code before}는 스냅샷 값, {@code after}는 현재 값이며 둘 다 JSON 값(null 가능).
 *
 * @param changeKind ADDED(스냅샷에선 비어 있었는데 지금은 값이 있음) / REMOVED(값이 있었는데 지금은 비어 있음,
 *                   대상 자체가 삭제된 경우 포함) / MODIFIED(값이 다른 값으로 바뀜)
 */
public record BaselineFieldChange(
        String field,
        BaselineFieldChangeKind changeKind,
        JsonNode before,
        JsonNode after) {
}
