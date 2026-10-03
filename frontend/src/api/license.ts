import { apiClient } from './client';
import type { PageResponse } from '../types/common';
import type { LicenseDetail, LicenseSummary, PublicLicenseStatus } from '../types/license';

export async function uploadLicense(file: File): Promise<LicenseDetail> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await apiClient.post<LicenseDetail>('/admin/licenses', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function getCurrentLicense(): Promise<LicenseDetail> {
  const { data } = await apiClient.get<LicenseDetail>('/admin/licenses/current');
  return data;
}

export async function listLicenseHistory(page = 0, size = 20): Promise<PageResponse<LicenseSummary>> {
  const { data } = await apiClient.get<PageResponse<LicenseSummary>>('/admin/licenses', { params: { page, size } });
  return data;
}

export async function getPublicLicenseStatus(): Promise<PublicLicenseStatus> {
  const { data } = await apiClient.get<PublicLicenseStatus>('/public/license-status');
  return data;
}
