package com.lightalm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.repository.UserRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * ADR-016 D1. 필터가 교체한 authorities를 이후 {@code @PreAuthorize("hasRole('ADMIN')")}가 실제로 보는지
 * 메서드 보안 프록시로 검증한다(로그인 시점 ADMIN이던 세션이 강등 후 거부됨).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SessionPrincipalRefreshMethodSecurityTest.TestConfig.class)
class SessionPrincipalRefreshMethodSecurityTest {

    static class AdminOnlyOperation {
        @PreAuthorize("hasRole('ADMIN')")
        public String run() {
            return "ok";
        }
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        AdminOnlyOperation adminOnlyOperation() {
            return new AdminOnlyOperation();
        }
    }

    @Autowired
    private AdminOnlyOperation adminOnlyOperation;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User user(SystemRole role) {
        return User.builder().id(33L).username("qa15u3").password("hash").email("qa@example.com")
                .fullName("QA").systemRole(role).enabled(true).build();
    }

    private AtomicReference<Throwable> runThroughFilter(SystemRole sessionRole, SystemRole dbRole) throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(33L)).thenReturn(Optional.of(user(dbRole)));
        SessionPrincipalRefreshFilter filter = new SessionPrincipalRefreshFilter(userRepository,
                new JsonAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules()),
                new HttpSessionSecurityContextRepository());

        UserPrincipal principal = new UserPrincipal(user(sessionRole));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setSession(new MockHttpSession());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            try {
                adminOnlyOperation.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        return failure;
    }

    @Test
    void demotedAdminSession_isDeniedByPreAuthorize() throws Exception {
        AtomicReference<Throwable> failure = runThroughFilter(SystemRole.ADMIN, SystemRole.USER);

        assertThat(failure.get()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void stillAdminSession_isAllowedByPreAuthorize() throws Exception {
        AtomicReference<Throwable> failure = runThroughFilter(SystemRole.ADMIN, SystemRole.ADMIN);

        assertThat(failure.get()).isNull();
    }

}
