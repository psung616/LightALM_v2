package com.lightalm.exception;

import com.lightalm.dto.ErrorResponse;
import com.lightalm.license.domain.LicenseFileRejectedException;
import com.lightalm.license.domain.LicenseInvalidException;
import com.lightalm.license.domain.LicenseSeatLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
    }

    /**
     * {@code @PreAuthorize}(예: {@code hasRole('ADMIN')})가 거부하면 Spring Security 6.3+에서
     * {@code AuthorizationDeniedException}(이 클래스의 상위 타입)이 컨트롤러 메서드 호출 중
     * 던져진다. 이 핸들러가 없으면 아래 {@link #handleUnexpected} catch-all로 떨어져 403 대신
     * 500을 반환한다 — ADR-014 구현 중 {@code ThemeAdminController}에서 실제로 재현/발견됐고,
     * {@code @PreAuthorize}를 쓰는 다른 모든 기존 컨트롤러(예: LicenseAdminController)에도
     * 동일하게 영향을 주는 전역 버그였다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.", request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(LicenseInvalidException.class)
    public ResponseEntity<ErrorResponse> handleLicenseInvalid(LicenseInvalidException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "LICENSE_INVALID", ex.getMessage(), request);
    }

    @ExceptionHandler(LicenseSeatLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLicenseSeatLimitExceeded(LicenseSeatLimitExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "LICENSE_SEAT_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    @ExceptionHandler(LicenseFileRejectedException.class)
    public ResponseEntity<ErrorResponse> handleLicenseFileRejected(LicenseFileRejectedException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "예기치 못한 오류가 발생했습니다.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
