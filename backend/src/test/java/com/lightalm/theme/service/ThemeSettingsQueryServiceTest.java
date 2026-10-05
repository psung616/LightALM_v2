package com.lightalm.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.theme.domain.SystemThemeSettings;
import com.lightalm.theme.domain.ThemeColorPreset;
import com.lightalm.theme.dto.PublicThemeResponse;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.repository.SystemThemeSettingsRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-014 §4/§7. 공개/관리자 조회 응답 분리와, 행이 없을 때 DEFAULT로 폴백하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ThemeSettingsQueryServiceTest {

    @Mock
    private SystemThemeSettingsRepository systemThemeSettingsRepository;

    @InjectMocks
    private ThemeSettingsQueryService themeSettingsQueryService;

    @Test
    void getPublicTheme_whenRowExists_returnsStoredPreset() {
        User admin = User.builder().id(1L).username("admin1").password("hash").email("a@example.com")
                .fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        SystemThemeSettings settings = SystemThemeSettings.builder()
                .id(1L).colorPreset(ThemeColorPreset.RED).updatedBy(admin).updatedAt(LocalDateTime.now()).build();
        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        PublicThemeResponse response = themeSettingsQueryService.getPublicTheme();

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.RED);
    }

    @Test
    void getPublicTheme_whenRowMissing_fallsBackToDefault() {
        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        PublicThemeResponse response = themeSettingsQueryService.getPublicTheme();

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.DEFAULT);
    }

    @Test
    void getAdminSettings_returnsUpdatedByFullNameAndAvailablePresets() {
        User admin = User.builder().id(1L).username("admin1").password("hash").email("a@example.com")
                .fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 5, 12, 0);
        SystemThemeSettings settings = SystemThemeSettings.builder()
                .id(1L).colorPreset(ThemeColorPreset.BLUE).updatedBy(admin).updatedAt(updatedAt).build();
        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        ThemeSettingsResponse response = themeSettingsQueryService.getAdminSettings();

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.BLUE);
        assertThat(response.updatedByFullName()).isEqualTo("Admin One");
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.availablePresets()).hasSize(5);
        assertThat(response.availablePresets()).extracting("code")
                .containsExactlyInAnyOrder("DEFAULT", "RED", "BLUE", "GREEN", "PURPLE");
    }

    @Test
    void getAdminSettings_whenRowMissing_fallsBackToDefaultWithoutUpdatedBy() {
        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        ThemeSettingsResponse response = themeSettingsQueryService.getAdminSettings();

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.DEFAULT);
        assertThat(response.updatedByFullName()).isNull();
    }
}
