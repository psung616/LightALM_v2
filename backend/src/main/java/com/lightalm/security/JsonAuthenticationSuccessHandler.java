package com.lightalm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.dto.ErrorResponse;
import com.lightalm.dto.UserResponse;
import com.lightalm.license.domain.LicenseInvalidException;
import com.lightalm.license.service.LicenseEnforcementService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * ADR-011 §2.3 게이트 A(라이센스 유효성): 로그인 성공 직후 비-ADMIN 사용자에 대해 활성 라이센스를 검사한다.
 * 시스템 ADMIN은 항상 통과시킨다(라이센스가 없어도 관리자가 로그인해서 새 라이센스를 올릴 수 있는 복구 경로).
 */
@Component
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final LicenseEnforcementService licenseEnforcementService;

    public JsonAuthenticationSuccessHandler(ObjectMapper objectMapper, LicenseEnforcementService licenseEnforcementService) {
        this.objectMapper = objectMapper;
        this.licenseEnforcementService = licenseEnforcementService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        if (!principal.isAdmin()) {
            try {
                licenseEnforcementService.requireActiveLicense();
            } catch (LicenseInvalidException ex) {
                rejectWithInvalidLicense(request, response, ex.getMessage());
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(UserResponse.from(principal)));
    }

    private void rejectWithInvalidLicense(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("LICENSE_INVALID")
                .message(message)
                .path(request.getRequestURI())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
