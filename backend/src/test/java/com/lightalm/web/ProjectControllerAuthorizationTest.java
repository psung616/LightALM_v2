package com.lightalm.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateProjectRequest;
import com.lightalm.dto.ProjectResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.DashboardService;
import com.lightalm.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * ADR-015 D6/DoD 16e. {@code POST /api/projects}({@link ProjectController#create})의
 * {@code @PreAuthorize("hasRole('ADMIN')")}가 System Admin만 허용하고 USER를 거부하는지,
 * 메서드 보안 프록시를 통해 검증한다({@code ThemeAdminControllerAuthorizationTest}와 같은 방식).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProjectControllerAuthorizationTest.TestConfig.class)
class ProjectControllerAuthorizationTest {

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {

        @Bean
        ProjectService projectService() {
            ProjectService mocked = mock(ProjectService.class);
            when(mocked.create(any(CreateProjectRequest.class), any(UserPrincipal.class)))
                    .thenReturn(ProjectResponse.builder().id(10L).projectKey("NEWPRJ").name("New Project").build());
            return mocked;
        }

        @Bean
        DashboardService dashboardService() {
            return mock(DashboardService.class);
        }

        @Bean
        ProjectController projectController(ProjectService projectService, DashboardService dashboardService) {
            return new ProjectController(projectService, dashboardService);
        }
    }

    @Autowired
    private ProjectController projectController;

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

    private static CreateProjectRequest request() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setProjectKey("NEWPRJ");
        request.setName("New Project");
        return request;
    }

    @Test
    void create_asNonAdminUser_isDeniedByMethodSecurity() {
        UserPrincipal nonAdmin = principalOf(SystemRole.USER);
        authenticateAs(nonAdmin);

        assertThatThrownBy(() -> projectController.create(request(), nonAdmin))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_asAdmin_isAllowed() {
        UserPrincipal admin = principalOf(SystemRole.ADMIN);
        authenticateAs(admin);

        ProjectResponse response = projectController.create(request(), admin);

        assertThat(response.getProjectKey()).isEqualTo("NEWPRJ");
    }

    @Test
    void list_asNonAdminUser_isStillAllowed() {
        // 목록 조회는 시스템 역할 검사 대상이 아님(멤버인 프로젝트만 보임) — 생성만 ADMIN 전용.
        UserPrincipal nonAdmin = principalOf(SystemRole.USER);
        authenticateAs(nonAdmin);

        projectController.list(nonAdmin, org.springframework.data.domain.Pageable.unpaged());
    }
}
