package com.lightalm.theme.dto;

import com.lightalm.theme.domain.ThemeColorPreset;

/**
 * GET /api/public/theme (공개, ADR-014 §4). 색상 프리셋 코드만 반환하고 민감 정보는
 * 담지 않는다 — 앱 부트스트랩이 비인증 상태(로그인/회원가입 화면 포함)에서도 호출한다.
 */
public record PublicThemeResponse(ThemeColorPreset colorPreset) {
}
