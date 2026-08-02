package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangeRequirementStatusRequest;
import com.lightalm.dto.CreateRequirementRequest;
import com.lightalm.dto.RequirementResponse;
import com.lightalm.dto.UpdateRequirementRequest;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.RequirementRepository;
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
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private RequirementService requirementService;

    private UserPrincipal principal;
    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("member1")
                .password("hash")
                .email("member1@example.com")
                .fullName("Member One")
                .systemRole(SystemRole.USER)
                .enabled(true)
                .build();
        principal = new UserPrincipal(user);
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
    }

    @Test
    void create_generatesKeyAndPersistsRequirement() {
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("로그인 기능");
        request.setType(RequirementType.FUNCTIONAL);

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectService.nextRequirementKey(10L)).thenReturn("LALM-R1");
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(invocation -> {
            Requirement r = invocation.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(java.time.LocalDateTime.now());
            r.setUpdatedAt(java.time.LocalDateTime.now());
            return r;
        });

        RequirementResponse response = requirementService.create(10L, request, principal);

        assertThat(response.getReqKey()).isEqualTo("LALM-R1");
        assertThat(response.getStatus()).isEqualTo(RequirementStatus.DRAFT);
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.MEMBER);
    }

    @Test
    void update_rejectsSelfAsParent() {
        UpdateRequirementRequest request = new UpdateRequirementRequest();
        request.setTitle("변경된 제목");
        request.setType(RequirementType.FUNCTIONAL);
        request.setParentRequirementId(5L);

        Requirement existing = Requirement.builder().id(5L).project(project).reqKey("LALM-R5").build();
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> requirementService.update(10L, 5L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void delete_requiresProjectAdminRole() {
        Requirement existing = Requirement.builder().id(7L).project(project).reqKey("LALM-R7").build();
        when(requirementRepository.findById(7L)).thenReturn(Optional.of(existing));
        doNothing().when(requirementRepository).delete(existing);

        requirementService.delete(10L, 7L, principal);

        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
        verify(requirementRepository).delete(existing);
    }

    @Test
    void changeStatus_updatesStatus() {
        Requirement existing = Requirement.builder().id(8L).project(project).reqKey("LALM-R8")
                .status(RequirementStatus.DRAFT).build();
        when(requirementRepository.findById(8L)).thenReturn(Optional.of(existing));
        ChangeRequirementStatusRequest request = new ChangeRequirementStatusRequest();
        request.setStatus(RequirementStatus.APPROVED);

        RequirementResponse response = requirementService.changeStatus(10L, 8L, request, principal);

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.APPROVED);
    }
}
