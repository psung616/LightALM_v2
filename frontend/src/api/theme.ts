import { apiClient } from './client';
import type { PublicTheme, ThemeColorPreset, ThemeSettings } from '../types/theme';

export async function getPublicTheme(): Promise<PublicTheme> {
  const { data } = await apiClient.get<PublicTheme>('/public/theme');
  return data;
}

export async function getThemeSettings(): Promise<ThemeSettings> {
  const { data } = await apiClient.get<ThemeSettings>('/admin/theme-settings');
  return data;
}

export async function updateThemeSettings(colorPreset: ThemeColorPreset): Promise<ThemeSettings> {
  const { data } = await apiClient.put<ThemeSettings>('/admin/theme-settings', { colorPreset });
  return data;
}
