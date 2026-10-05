package com.lightalm.theme.dto;

import com.lightalm.theme.domain.ThemeColorPreset;
import java.util.Arrays;
import java.util.List;

/**
 * ADR-014 §4. 관리자 화면 스와치 미리보기용 — 선택 가능한 프리셋 코드/라벨/대표 색상.
 */
public record ThemePresetOption(String code, String label, String primaryColor) {

    public static List<ThemePresetOption> all() {
        return Arrays.stream(ThemeColorPreset.values())
                .map(preset -> new ThemePresetOption(preset.name(), preset.getLabel(), preset.getPrimaryColor()))
                .toList();
    }
}
