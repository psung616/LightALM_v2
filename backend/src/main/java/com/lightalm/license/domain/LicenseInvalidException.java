package com.lightalm.license.domain;

/**
 * ADR-011 §2.3 게이트 A(라이센스 유효성): 활성 라이센스가 없거나 만료된 경우.
 * GlobalExceptionHandler가 403 LICENSE_INVALID로 변환한다.
 */
public class LicenseInvalidException extends RuntimeException {

    public LicenseInvalidException(String message) {
        super(message);
    }
}
