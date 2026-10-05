package com.lightalm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * ADR-016 D1. 세션 principal 재검증 필터 단위 테스트.
 */
class SessionPrincipalRefreshFilterTest {

    private UserRepository userRepository;
    private SessionPrincipalRefreshFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        JsonAuthenticationEntryPoint entryPoint =
                new JsonAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules());
        filter = new SessionPrincipalRefreshFilter(userRepository, entryPoint, new HttpSessionSecurityContextRepository());
        session = new MockHttpSession();
        request = new MockHttpServletRequest("PUT", "/api/users/33");
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User user(SystemRole role, boolean enabled) {
        return User.builder()
                .id(33L).username("qa15u3").password("hash").email("qa15u3@example.com")
                .fullName("QA").systemRole(role).enabled(enabled).build();
    }

    private static void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @Test
    void disabledUser_invalidatesSessionAndReturns401WithoutContinuingChain() throws Exception {
        authenticateAs(user(SystemRole.ADMIN, true));
        when(userRepository.findById(33L)).thenReturn(Optional.of(user(SystemRole.USER, false)));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void deletedUser_invalidatesSessionAndReturns401() throws Exception {
        authenticateAs(user(SystemRole.USER, true));
        when(userRepository.findById(33L)).thenReturn(Optional.empty());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(session.isInvalid()).isTrue();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void demotedAdmin_getsRefreshedPrincipalAndAuthoritiesSavedToSession() throws Exception {
        authenticateAs(user(SystemRole.ADMIN, true));
        when(userRepository.findById(33L)).thenReturn(Optional.of(user(SystemRole.USER, true)));
        AtomicReference<Authentication> seenByChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenByChain.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        Authentication seen = seenByChain.get();
        assertThat(seen).isNotNull();
        assertThat(seen.isAuthenticated()).isTrue();
        assertThat(((UserPrincipal) seen.getPrincipal()).getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(AuthorityUtils.authorityListToSet(seen.getAuthorities())).containsExactly("ROLE_USER");

        SecurityContext saved = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(saved).isNotNull();
        assertThat(((UserPrincipal) saved.getAuthentication().getPrincipal()).getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(session.isInvalid()).isFalse();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void promotedUser_getsAdminAuthority() throws Exception {
        authenticateAs(user(SystemRole.USER, true));
        when(userRepository.findById(33L)).thenReturn(Optional.of(user(SystemRole.ADMIN, true)));
        AtomicReference<Authentication> seenByChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenByChain.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertThat(seenByChain.get().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(((UserPrincipal) seenByChain.get().getPrincipal()).isAdmin()).isTrue();
    }

    @Test
    void unchangedActiveUser_passesThroughWithSameAuthenticationAndNoSessionWrite() throws Exception {
        authenticateAs(user(SystemRole.ADMIN, true));
        Authentication before = SecurityContextHolder.getContext().getAuthentication();
        when(userRepository.findById(33L)).thenReturn(Optional.of(user(SystemRole.ADMIN, true)));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(before);
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void anonymousRequest_passesThroughWithoutDbLookup() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void noAuthentication_passesThroughWithoutDbLookup() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }
}
