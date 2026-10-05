package com.lightalm.theme.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.security.UserPrincipal;
import com.lightalm.theme.domain.ThemeColorPreset;
import com.lightalm.theme.dto.PublicThemeResponse;
import com.lightalm.theme.dto.ThemeSettingsResponse;
import com.lightalm.theme.dto.UpdateThemeSettingsRequest;
import com.lightalm.theme.service.ThemeSettingsCommandService;
import com.lightalm.theme.service.ThemeSettingsQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * ADR-014 §4/§7. {@code @PreAuthorize("hasRole('ADMIN')")}가 실제로 시스템 ADMIN이 아닌
 * 사용자(일반 {@code USER})를 거부하고 ADMIN은 허용하는지, 메서드 보안 프록시를 통해 검증한다.
 *
 * <p>프로젝트 전역에 이 패턴의 선례가 없어(기존 ADMIN 전용 컨트롤러들은 MockMvc/보안 통합
 * 테스트가 없다), {@code @WebMvcTest} 대신 최소한의 {@code @EnableMethodSecurity} 컨텍스트로
 * 컨트롤러 빈을 직접 호출해 AOP 프록시가 거부/허용을 강제하는지만 확인한다 — DB/Testcontainers가
 * 전혀 필요 없어 {@code mvn test}에서 그대로 실행된다.</p>
 *
 * <p>참고: 시스템 역할은 {@code ADMIN}/{@code USER} 둘뿐이다(GLOSSARY.md §3). 프로젝트 역할
 * {@code MEMBER}/{@code VIEWER}는 이 시스템 전역 API와 무관하다 — 비-ADMIN의 대표값으로
 * {@code USER}를 사용한다.</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ThemeAdminControllerAuthorizationTest.TestConfig.class)
class ThemeAdminControllerAuthorizationTest {

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {

        @Bean
        ThemeSettingsQueryService themeSettingsQueryService() {
            ThemeSettingsQueryService mocked = mock(ThemeSettingsQueryService.class);
            when(mocked.getAdminSettings()).thenReturn(
                    new ThemeSettingsResponse(ThemeColorPreset.DEFAULT, null, null, List.of()));
            return mocked;
        }

        @Bean
        ThemeSettingsCommandService themeSettingsCommandService() {
            ThemeSettingsCommandService mocked = mock(ThemeSettingsCommandService.class);
            when(mocked.updatePreset(any(UpdateThemeSettingsRequest.class), any(UserPrincipal.class)))
                    .thenReturn(new ThemeSettingsResponse(ThemeColorPreset.RED, "Admin One", null, List.of()));
            return mocked;
        }

        @Bean
        ThemeAdminController themeAdminController(ThemeSettingsQueryService queryService,
                                                    ThemeSettingsCommandService commandService) {
            return new ThemeAdminController(queryService, commandService);
        }
    }

    @Autowired
    private ThemeAdminController themeAdminController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static UserPrincipal principalOf(SystemRole systemRole) {
        User user = User.builder()
                .id(1L).username("tester").password("hash").email("tester@example.com")
                .fullName("Tester").systemRole(systemRole).enabled(true).build();
        return new UserPrincipal(user);
    }

    private static void authenticateAs(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void getSettings_asNonAdminUser_isDeniedByMethodSecurity() {
        authenticateAs(principalOf(SystemRole.USER));

        assertThatThrownBy(themeAdminController::getSettings)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateSettings_asNonAdminUser_isDeniedByMethodSecurity() {
        UserPrincipal nonAdmin = principalOf(SystemRole.USER);
        authenticateAs(nonAdmin);

        assertThatThrownBy(() -> themeAdminController.updateSettings(
                new UpdateThemeSettingsRequest("RED"), nonAdmin))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getSettings_asAdmin_isAllowed() {
        authenticateAs(principalOf(SystemRole.ADMIN));

        ThemeSettingsResponse response = themeAdminController.getSettings();

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.DEFAULT);
    }

    @Test
    void updateSettings_asAdmin_isAllowed() {
        UserPrincipal admin = principalOf(SystemRole.ADMIN);
        authenticateAs(admin);

        ThemeSettingsResponse response = themeAdminController.updateSettings(
                new UpdateThemeSettingsRequest("RED"), admin);

        assertThat(response.colorPreset()).isEqualTo(ThemeColorPreset.RED);
    }
}
