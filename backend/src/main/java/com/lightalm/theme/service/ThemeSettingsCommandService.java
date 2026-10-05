package com.lightalm.theme.service;

import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.theme.domain.SystemThemeSettings;
import com.lightalm.theme.domain.ThemeColorPreset;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.dto.UpdateThemeSettingsRequest;
import com.lightalm.theme.repository.SystemThemeSettingsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-014 §4/§7. 시스템 전역 색상 프리셋 변경 — ADMIN만 호출(컨트롤러의
 * {@code @PreAuthorize("hasRole('ADMIN')")}가 권한을 강제한다).
 */
@Service
@RequiredArgsConstructor
public class ThemeSettingsCommandService {

    private final SystemThemeSettingsRepository systemThemeSettingsRepository;
    private final UserRepository userRepository;

    @Transactional
    public ThemeSettingsResponse updatePreset(UpdateThemeSettingsRequest request, UserPrincipal principal) {
        ThemeColorPreset preset = parsePreset(request.colorPreset());
        User updatedBy = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        SystemThemeSettings settings = systemThemeSettingsRepository.findById(ThemeSettingsQueryService.SINGLETON_ID)
                .orElseGet(() -> systemThemeSettingsRepository.save(SystemThemeSettings.builder()
                        .id(ThemeSettingsQueryService.SINGLETON_ID)
                        .colorPreset(ThemeColorPreset.DEFAULT)
                        .updatedAt(LocalDateTime.now())
                        .build()));

        settings.changeColorPreset(preset, updatedBy);
        return ThemeSettingsResponse.from(settings);
    }

    private ThemeColorPreset parsePreset(String colorPreset) {
        try {
            return ThemeColorPreset.valueOf(colorPreset);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("허용되지 않은 색상 프리셋입니다: " + colorPreset);
        }
    }
}
