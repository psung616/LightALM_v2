package com.lightalm.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.theme.domain.SystemThemeSettings;
import com.lightalm.theme.domain.ThemeColorPreset;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.dto.UpdateThemeSettingsRequest;
import com.lightalm.theme.repository.SystemThemeSettingsRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-014 §4/§7. 프리셋 변경 성공, 허용되지 않은 값 거부(DEFAULT 프리셋이 index.css와 동일함을
 * 전제로 하는 하위 호환 검증 포함), 시드 행이 없을 때의 방어적 생성 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ThemeSettingsCommandServiceTest {

    @Mock
    private SystemThemeSettingsRepository systemThemeSettingsRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ThemeSettingsCommandService themeSettingsCommandService;

    @Test
    void updatePreset_success_changesColorPresetAndRecordsUpdatedBy() {
        User admin = User.builder().id(1L).username("admin1").password("hash").email("a@example.com")
                .fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(admin);
        SystemThemeSettings settings = SystemThemeSettings.builder()
                .id(1L).colorPreset(ThemeColorPreset.DEFAULT).updatedAt(LocalDateTime.now()).build();

        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ThemeSettingsResponse response = themeSettingsCommandService.updatePreset(
                new UpdateThemeSettingsRequest("RED"), principal);

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.RED);
        assertThat(response.updatedByFullName()).isEqualTo("Admin One");
    }

    @Test
    void updatePreset_withDefaultPreset_matchesExistingIndexCssValues() {
        // DEFAULT 프리셋 선택 시 ADR-014 §2가 명시한 "기존 index.css 값과 100% 동일" 요구를
        // 코드 레벨에서 간접 검증한다 — enum 자체의 primaryColor가 index.css --color-primary 값과 같아야 한다.
        assertThat(ThemeColorPreset.DEFAULT.getPrimaryColor()).isEqualTo("#5e6ad2");
    }

    @Test
    void updatePreset_whenColorPresetValueNotAllowed_throwsValidationException() {
        User admin = User.builder().id(1L).username("admin1").password("hash").email("a@example.com")
                .fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(admin);

        assertThatThrownBy(() -> themeSettingsCommandService.updatePreset(
                new UpdateThemeSettingsRequest("GOLD"), principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updatePreset_whenSeedRowMissing_createsSingletonRowDefensively() {
        User admin = User.builder().id(1L).username("admin1").password("hash").email("a@example.com")
                .fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        UserPrincipal principal = new UserPrincipal(admin);

        when(systemThemeSettingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(systemThemeSettingsRepository.save(org.mockito.ArgumentMatchers.any(SystemThemeSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ThemeSettingsResponse response = themeSettingsCommandService.updatePreset(
                new UpdateThemeSettingsRequest("GREEN"), principal);

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.GREEN);
    }
}
