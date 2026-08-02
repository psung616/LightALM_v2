package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectMember;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.repository.ProjectMemberRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private UserPrincipal systemAdmin;
    private UserPrincipal regularMember;

    @BeforeEach
    void setUp() {
        User admin = User.builder().id(1L).username("admin").password("hash").email("admin@example.com")
                .fullName("Admin").systemRole(SystemRole.ADMIN).enabled(true).build();
        systemAdmin = new UserPrincipal(admin);

        User member = User.builder().id(2L).username("member").password("hash").email("member@example.com")
                .fullName("Member").systemRole(SystemRole.USER).enabled(true).build();
        regularMember = new UserPrincipal(member);
    }

    @Test
    void requireRole_systemAdminAlwaysPasses_evenWithoutMembership() {
        // 시스템 ADMIN은 프로젝트 멤버가 아니어도 항상 PROJECT_ADMIN 이상으로 취급된다(§6).
        assertThatCode(() -> projectMemberService.requireRole(100L, systemAdmin, ProjectRole.PROJECT_ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void requireRole_nonMemberIsForbidden() {
        when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.requireRole(100L, regularMember, ProjectRole.VIEWER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireRole_viewerCannotSatisfyMemberRequirement() {
        ProjectMember membership = ProjectMember.builder().role(ProjectRole.VIEWER).build();
        when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> projectMemberService.requireRole(100L, regularMember, ProjectRole.MEMBER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireRole_memberSatisfiesViewerRequirement() {
        ProjectMember membership = ProjectMember.builder().role(ProjectRole.MEMBER).build();
        when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.of(membership));

        assertThatCode(() -> projectMemberService.requireRole(100L, regularMember, ProjectRole.VIEWER))
                .doesNotThrowAnyException();
    }

    @Test
    void registerProjectAdmin_savesMemberWithProjectAdminRole() {
        Project project = Project.builder().id(10L).projectKey("LALM").build();
        User user = User.builder().id(2L).username("member").build();

        projectMemberService.registerProjectAdmin(project, user);

        org.mockito.ArgumentCaptor<ProjectMember> captor = org.mockito.ArgumentCaptor.forClass(ProjectMember.class);
        org.mockito.Mockito.verify(projectMemberRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
    }
}
