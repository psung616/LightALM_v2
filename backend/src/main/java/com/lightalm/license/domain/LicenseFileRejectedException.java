package com.lightalm.license.domain;

import lombok.Getter;

/**
 * ADR-011 §2.3: 업로드된 라이센스 파일이 파싱/서명/만료 검증을 통과하지 못해 저장 없이 거부된 경우.
 * errorCode는 LICENSE_SIGNATURE_INVALID / LICENSE_ALREADY_EXPIRED / LICENSE_FILE_TOO_LARGE 중 하나이며
 * GlobalExceptionHandler가 400 응답의 error 필드로 그대로 노출한다.
 */
@Getter
public class LicenseFileRejectedException extends RuntimeException {

    private final String errorCode;

    public LicenseFileRejectedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
