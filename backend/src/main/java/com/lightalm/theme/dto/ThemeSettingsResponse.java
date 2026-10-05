package com.lightalm.theme.dto;

import com.lightalm.theme.domain.SystemThemeSettings;
import com.lightalm.theme.domain.ThemeColorPreset;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET/PUT /api/admin/theme-settings (ADMIN, ADR-014 §4). 현재 설정 상세 +
 * 선택 가능한 프리셋 전체 목록(관리자 화면 스와치 미리보기용).
 */
public record ThemeSettingsResponse(
        ThemeColorPreset colorPreset,
        String updatedByFullName,
        LocalDateTime updatedAt,
        List<ThemePresetOption> availablePresets
) {

    public static ThemeSettingsResponse from(SystemThemeSettings settings) {
        String updatedByFullName = settings.getUpdatedBy() != null ? settings.getUpdatedBy().getFullName() : null;
        return new ThemeSettingsResponse(
                settings.getColorPreset(), updatedByFullName, settings.getUpdatedAt(), ThemePresetOption.all());
    }

    public static ThemeSettingsResponse defaultFallback() {
        return new ThemeSettingsResponse(ThemeColorPreset.DEFAULT, null, null, ThemePresetOption.all());
    }
}
