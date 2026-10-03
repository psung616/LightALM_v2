package com.lightalm.license.dto;

/**
 * GET /api/public/license-status (공개). /signup 화면이 폼 노출 전 확인하는 최소 정보.
 * 시트 수/라이센스 키 등 민감 정보는 담지 않는다(ADR-011 §2.4).
 */
public record PublicLicenseStatusResponse(boolean signupAllowed, String reason) {
}
