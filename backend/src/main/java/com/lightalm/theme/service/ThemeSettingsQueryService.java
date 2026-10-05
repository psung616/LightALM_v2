package com.lightalm.theme.service;

import com.lightalm.theme.domain.SystemThemeSettings;
import com.lightalm.theme.domain.ThemeColorPreset;
import com.lightalm.theme.dto.PublicThemeResponse;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.repository.SystemThemeSettingsRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-014 §4/§7. 공개 조회(비인증)와 관리자 조회(ADMIN) 두 응답을 분리해 제공한다.
 *
 * <p>행이 없는 극단적 상황(마이그레이션 시드 실패 등)에 대비해 조회 실패 시 {@code DEFAULT}로
 * 폴백한다 — 공개 API가 500을 반환해 로그인 화면 자체가 깨지는 상황을 막기 위함(ADR-014 §7).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeSettingsQueryService {

    static final Long SINGLETON_ID = 1L;

    private final SystemThemeSettingsRepository systemThemeSettingsRepository;

    public PublicThemeResponse getPublicTheme() {
        ThemeColorPreset preset = systemThemeSettingsRepository.findById(SINGLETON_ID)
                .map(SystemThemeSettings::getColorPreset)
                .orElse(ThemeColorPreset.DEFAULT);
        return new PublicThemeResponse(preset);
    }

    public ThemeSettingsResponse getAdminSettings() {
        Optional<SystemThemeSettings> settings = systemThemeSettingsRepository.findById(SINGLETON_ID);
        return settings.map(ThemeSettingsResponse::from).orElseGet(ThemeSettingsResponse::defaultFallback);
    }
}
