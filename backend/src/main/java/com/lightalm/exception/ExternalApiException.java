package com.lightalm.exception;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private final String errorCode;

    public ExternalApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
