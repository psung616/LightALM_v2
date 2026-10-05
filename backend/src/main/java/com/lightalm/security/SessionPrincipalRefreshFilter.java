package com.lightalm.security;

import com.lightalm.domain.User;
import com.lightalm.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ADR-016 D1. 세션에 저장된 {@link UserPrincipal}의 활성 여부·시스템 역할을 요청마다 DB 값으로 재검증한다.
 *
 * <ul>
 *   <li>사용자 행이 없거나 비활성화됨 → 세션 무효화 + 401(체인 중단)</li>
 *   <li>시스템 역할이 바뀜 → DB 값으로 principal/authorities를 교체하고 세션에도 저장</li>
 *   <li>그 외 → 그대로 통과</li>
 * </ul>
 *
 * <p>스프링 빈으로 등록하지 않는다(서블릿 필터로 자동 이중 등록되지 않도록 {@code SecurityConfig}에서 생성).</p>
 */
public class SessionPrincipalRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityContextRepository securityContextRepository;

    public SessionPrincipalRefreshFilter(UserRepository userRepository,
                                         AuthenticationEntryPoint authenticationEntryPoint,
                                         SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal sessionPrincipal)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> current = userRepository.findById(sessionPrincipal.getId());
        if (current.isEmpty() || !Boolean.TRUE.equals(current.get().getEnabled())) {
            rejectInactiveUser(request, response);
            return;
        }

        User user = current.get();
        if (user.getSystemRole() != sessionPrincipal.getSystemRole()) {
            replacePrincipal(user, authentication, request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void rejectInactiveUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response,
                new InsufficientAuthenticationException("비활성화되었거나 삭제된 사용자의 세션입니다."));
    }

    private void replacePrincipal(User user, Authentication previous, HttpServletRequest request,
                                  HttpServletResponse response) {
        UserPrincipal refreshed = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.authenticated(refreshed, null, refreshed.getAuthorities());
        token.setDetails(previous.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
