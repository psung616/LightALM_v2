package com.lightalm.theme.api;

import com.lightalm.security.UserPrincipal;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.dto.UpdateThemeSettingsRequest;
import com.lightalm.theme.service.ThemeSettingsCommandService;
import com.lightalm.theme.service.ThemeSettingsQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-014 §4/§7. 시스템 테마(색상 프리셋) 설정 조회/변경 — 시스템 ADMIN 전용
 * ({@code UserController}와 동일한 권한 패턴).
 */
@RestController
@RequestMapping("/api/admin/theme-settings")
@RequiredArgsConstructor
public class ThemeAdminController {

    private final ThemeSettingsQueryService themeSettingsQueryService;
    private final ThemeSettingsCommandService themeSettingsCommandService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ThemeSettingsResponse getSettings() {
        return themeSettingsQueryService.getAdminSettings();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ThemeSettingsResponse updateSettings(@Valid @RequestBody UpdateThemeSettingsRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return themeSettingsCommandService.updatePreset(request, principal);
    }
}
