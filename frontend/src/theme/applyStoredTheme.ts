import type { ThemeColorPreset } from '../types/theme';

/**
 * ADR-014 §5.1/§5.2. 색상 프리셋은 조직 설정(서버 보관, `GET /api/public/theme`으로 동기화),
 * 라이트/다크 모드는 개인 선호(서버 미보관, `localStorage`만 사용)다. 이 모듈은 두 값을
 * 네트워크 요청 없이 동기적으로 읽어 `<html>` 엘리먼트에 즉시 반영한다 — 리액트 렌더링 전에
 * 호출해야 첫 페인트부터 올바른 색이 보인다(깜빡임/FOUC 방지).
 */

const COLOR_PRESET_STORAGE_KEY = 'lightalm:colorPreset';
const COLOR_MODE_STORAGE_KEY = 'lightalm:colorMode';

export type ColorMode = 'light' | 'dark';

const VALID_PRESETS: ThemeColorPreset[] = ['DEFAULT', 'RED', 'BLUE', 'GREEN', 'PURPLE'];

function isThemeColorPreset(value: string | null): value is ThemeColorPreset {
  return value !== null && (VALID_PRESETS as string[]).includes(value);
}

export function getStoredColorPreset(): ThemeColorPreset {
  try {
    const stored = localStorage.getItem(COLOR_PRESET_STORAGE_KEY);
    return isThemeColorPreset(stored) ? stored : 'DEFAULT';
  } catch {
    // localStorage를 쓸 수 없는 환경(프라이버시 모드 등) — DEFAULT로 안전하게 폴백.
    return 'DEFAULT';
  }
}

export function getStoredColorMode(): ColorMode {
  try {
    const stored = localStorage.getItem(COLOR_MODE_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
  } catch {
    // ignore
  }
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return 'light';
}

export function storeColorPreset(colorPreset: ThemeColorPreset): void {
  try {
    localStorage.setItem(COLOR_PRESET_STORAGE_KEY, colorPreset);
  } catch {
    // ignore — 이번 세션 동안만 적용되고 새로고침 시 DEFAULT로 되돌아간다.
  }
}

export function storeColorMode(colorMode: ColorMode): void {
  try {
    localStorage.setItem(COLOR_MODE_STORAGE_KEY, colorMode);
  } catch {
    // ignore
  }
}

/** DEFAULT는 속성 자체를 생략한다(ADR-014 §5.1 — index.css 기본값이 곧 DEFAULT 프리셋이므로). */
export function applyColorPreset(colorPreset: ThemeColorPreset): void {
  const root = document.documentElement;
  if (colorPreset === 'DEFAULT') {
    delete root.dataset.themeColor;
  } else {
    root.dataset.themeColor = colorPreset.toLowerCase();
  }
}

export function applyColorMode(colorMode: ColorMode): void {
  document.documentElement.dataset.colorMode = colorMode;
}

/** `createRoot(...).render(...)` 호출 이전에 동기적으로 실행한다(main.tsx). */
export function applyStoredTheme(): void {
  applyColorPreset(getStoredColorPreset());
  applyColorMode(getStoredColorMode());
}
