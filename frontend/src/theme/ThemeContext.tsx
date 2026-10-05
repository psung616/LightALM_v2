import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { getPublicTheme } from '../api/theme';
import {
  applyColorMode,
  applyColorPreset,
  getStoredColorMode,
  getStoredColorPreset,
  storeColorMode,
  storeColorPreset,
  type ColorMode,
} from './applyStoredTheme';
import type { ThemeColorPreset } from '../types/theme';

interface ThemeContextValue {
  colorPreset: ThemeColorPreset;
  colorMode: ColorMode;
  /** PUT /api/admin/theme-settings 성공 후 전역 상태를 새로고침 없이 즉시 갱신하기 위해 호출한다. */
  setColorPreset: (preset: ThemeColorPreset) => void;
  toggleColorMode: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

/**
 * ADR-014 §5.2. 색상 프리셋은 조직 설정(서버 보관), 라이트/다크 모드는 개인 선호(localStorage만).
 * `main.tsx`가 렌더링 전에 호출하는 `applyStoredTheme()`로 이미 `<html>`에 반영된 값을 초기
 * 상태로 읽어오고, 마운트 후 `GET /api/public/theme`으로 서버 값과 동기화한다 — 이 호출은
 * 비인증 상태에서도 항상 성공하므로 `/login` 화면에도 동일하게 적용된다.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [colorPreset, setColorPresetState] = useState<ThemeColorPreset>(getStoredColorPreset);
  const [colorMode, setColorModeState] = useState<ColorMode>(getStoredColorMode);

  const setColorPreset = useCallback((preset: ThemeColorPreset) => {
    applyColorPreset(preset);
    storeColorPreset(preset);
    setColorPresetState(preset);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const serverTheme = await getPublicTheme();
        if (!cancelled) {
          setColorPreset(serverTheme.colorPreset);
        }
      } catch {
        // 공개 API 호출이 실패해도 로컬 캐시 값을 그대로 유지한다(화면이 깨지지 않도록).
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [setColorPreset]);

  const toggleColorMode = useCallback(() => {
    setColorModeState((prev) => {
      const next: ColorMode = prev === 'dark' ? 'light' : 'dark';
      applyColorMode(next);
      storeColorMode(next);
      return next;
    });
  }, []);

  return (
    <ThemeContext.Provider value={{ colorPreset, colorMode, setColorPreset, toggleColorMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return ctx;
}
