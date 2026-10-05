export type ThemeColorPreset = 'DEFAULT' | 'RED' | 'BLUE' | 'GREEN' | 'PURPLE';

/** GET /api/public/theme (공개). 색상 프리셋 코드만 담는다 — 민감 정보 없음. */
export interface PublicTheme {
  colorPreset: ThemeColorPreset;
}

/** 관리자 화면 스와치 미리보기용 — 선택 가능한 프리셋 코드/라벨/대표 색상. */
export interface ThemePresetOption {
  code: ThemeColorPreset;
  label: string;
  primaryColor: string;
}

/** GET/PUT /api/admin/theme-settings (ADMIN). */
export interface ThemeSettings {
  colorPreset: ThemeColorPreset;
  updatedByFullName: string | null;
  updatedAt: string | null;
  availablePresets: ThemePresetOption[];
}
