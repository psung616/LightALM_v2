package com.lightalm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.dto.UserResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인 성공 시 항상 200 + {@link UserResponse}를 반환한다.
 * 로그인 시점의 추가 게이트는 없다(ADR-015로 라이센스 게이트 제거, ADR-011 이전 동작으로 복귀).
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(UserResponse.from(principal)));
    }
}
