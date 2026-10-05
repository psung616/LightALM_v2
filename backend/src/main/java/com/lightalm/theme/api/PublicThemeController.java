package com.lightalm.theme.api;

import com.lightalm.theme.dto.PublicThemeResponse;
import com.lightalm.theme.service.ThemeSettingsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-014 §4. 공개 엔드포인트 — 앱 부트스트랩이 비인증 상태로 호출한다
 * (SecurityConfig의 permitAll 목록에 등록됨).
 */
@RestController
@RequestMapping("/api/public/theme")
@RequiredArgsConstructor
public class PublicThemeController {

    private final ThemeSettingsQueryService themeSettingsQueryService;

    @GetMapping
    public PublicThemeResponse getPublicTheme() {
        return themeSettingsQueryService.getPublicTheme();
    }
}
