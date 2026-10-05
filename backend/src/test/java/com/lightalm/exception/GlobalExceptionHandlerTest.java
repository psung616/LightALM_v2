package com.lightalm.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import com.lightalm.dto.ErrorResponse;

/**
 * ADR-014 구현 중 발견: {@code @PreAuthorize}가 거부하면 Spring Security 6.3+에서
 * {@code AuthorizationDeniedException}(AccessDeniedException의 하위 타입)이 던져지는데,
 * 전용 핸들러가 없으면 catch-all({@link GlobalExceptionHandler#handleUnexpected})로 떨어져
 * 403 대신 500이 응답되는 전역 버그가 있었다({@code ThemeAdminController}에서 실제 curl로
 * 재현, {@code LicenseAdminController} 등 기존 ADMIN 전용 컨트롤러에도 동일하게 영향).
 * 이 테스트는 그 수정(handleAccessDenied)이 403/FORBIDDEN을 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_withAuthorizationDeniedException_returns403Forbidden() {
        when(request.getRequestURI()).thenReturn("/api/admin/theme-settings");
        AccessDeniedException ex = new AuthorizationDeniedException("Access Denied", () -> false);

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleAccessDenied_withPlainAccessDeniedException_returns403Forbidden() {
        when(request.getRequestURI()).thenReturn("/api/admin/theme-settings");
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
