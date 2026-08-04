package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Release;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.AddReleaseItemRequest;
import com.lightalm.dto.CreateReleaseRequest;
import com.lightalm.dto.ReleaseResponse;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.ReleaseItemRepository;
import com.lightalm.repository.ReleaseRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    @Mock
    private ReleaseRepository releaseRepository;
    @Mock
    private ReleaseItemRepository releaseItemRepository;
    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private ReleaseService releaseService;

    private UserPrincipal principal;
    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("admin1")
                .password("hash")
                .email("admin1@example.com")
                .fullName("Admin One")
                .systemRole(SystemRole.USER)
                .enabled(true)
                .build();
        principal = new UserPrincipal(user);
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
    }

    @Test
    void create_generatesReleaseAndPersists() {
        CreateReleaseRequest request = new CreateReleaseRequest();
        request.setVersion("1.0.0");

        when(releaseRepository.existsByProjectIdAndVersion(10L, "1.0.0")).thenReturn(false);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(releaseRepository.save(any(Release.class))).thenAnswer(invocation -> {
            Release r = invocation.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });
        when(releaseItemRepository.findByReleaseId(100L)).thenReturn(Collections.emptyList());

        ReleaseResponse response = releaseService.create(10L, request, principal);

        assertThat(response.getVersion()).isEqualTo("1.0.0");
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
    }

    @Test
    void create_rejectsDuplicateVersion() {
        CreateReleaseRequest request = new CreateReleaseRequest();
        request.setVersion("1.0.0");

        when(releaseRepository.existsByProjectIdAndVersion(10L, "1.0.0")).thenReturn(true);

        assertThatThrownBy(() -> releaseService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addItem_rejectsTestCaseTargetType() {
        Release release = Release.builder().id(5L).project(project).version("1.0.0").build();
        when(releaseRepository.findById(5L)).thenReturn(Optional.of(release));

        AddReleaseItemRequest request = new AddReleaseItemRequest();
        request.setTargetType(TargetType.TEST_CASE);
        request.setTargetId(1L);

        assertThatThrownBy(() -> releaseService.addItem(10L, 5L, request, principal))
                .isInstanceOf(ValidationException.class);
    }
}
