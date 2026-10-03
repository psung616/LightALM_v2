package com.lightalm.license.domain;

/**
 * ADR-011 §2.3 게이트 B(시트 한도): enabled=true 사용자 수가 seat_limit에 도달한 경우.
 * GlobalExceptionHandler가 403 LICENSE_SEAT_LIMIT_EXCEEDED로 변환한다.
 */
public class LicenseSeatLimitExceededException extends RuntimeException {

    public LicenseSeatLimitExceededException(String message) {
        super(message);
    }
}
