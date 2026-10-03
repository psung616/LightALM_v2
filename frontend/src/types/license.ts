import type { LicenseStatus, LicenseType } from './common';

export interface LicenseSummary {
  id: number;
  licenseKey: string;
  organizationName: string;
  licenseType: LicenseType;
  status: LicenseStatus;
  seatLimit: number;
  rawFileName: string;
  uploadedByUsername: string | null;
  uploadedAt: string;
}

export interface LicenseDetail {
  id: number;
  licenseKey: string;
  organizationName: string;
  licenseType: LicenseType;
  status: LicenseStatus;
  seatLimit: number;
  seatsUsed: number;
  seatsRemaining: number;
  issuedAt: string;
  expiresAt: string | null;
  rawFileName: string;
  uploadedByUsername: string | null;
  uploadedAt: string;
}

export interface PublicLicenseStatus {
  signupAllowed: boolean;
  reason: string | null;
}
