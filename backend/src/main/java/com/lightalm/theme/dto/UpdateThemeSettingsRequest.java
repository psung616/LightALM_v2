package com.lightalm.theme.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PUT /api/admin/theme-settings 요청 바디(ADR-014 §4). {@code colorPreset}을 enum 타입이
 * 아니라 문자열로 받는 이유: Jackson이 알 수 없는 enum 값을 바인딩 단계에서 거부하면
 * {@code HttpMessageNotReadableException}으로 이어져 전역 핸들러가 500으로 처리하게 된다
 * (GlobalExceptionHandler에 해당 예외 전용 핸들러가 없음). 서비스 레이어에서 직접
 * {@link com.lightalm.theme.domain.ThemeColorPreset#valueOf}로 파싱해 실패 시
 * {@code ValidationException}(400)으로 일관되게 응답하기 위해 문자열로 받는다.
 */
public record UpdateThemeSettingsRequest(
        @NotBlank(message = "colorPreset은 필수입니다.") String colorPreset
) {
}
