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
import com.lightalm.enumeration.service.EnumerationValueValidator;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.workflow.service.WorkflowTransitionPolicy;
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
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EnumerationValueValidator enumerationValueValidator;
    @Mock
    private WorkflowTransitionPolicy workflowTransitionPolicy;

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
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM.name());
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.MEMBER);
        verify(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "MEDIUM");
    }

    @Test
    void create_whenEnumerationValidatorRejectsPriority_propagatesException() {
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("로그인 기능");
        request.setType(RequirementType.FUNCTIONAL);
        request.setPriority("BLOCKER");

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new ValidationException("유효하지 않은 PRIORITY 값입니다: BLOCKER"))
                .when(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "BLOCKER");

        assertThatThrownBy(() -> requirementService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
        verify(requirementRepository, org.mockito.Mockito.never()).save(any(Requirement.class));
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
                .status(RequirementStatus.APPROVED).build();
        when(requirementRepository.findById(8L)).thenReturn(Optional.of(existing));
        ChangeRequirementStatusRequest request = new ChangeRequirementStatusRequest();
        request.setStatus(RequirementStatus.IN_PROGRESS);

        RequirementResponse response = requirementService.changeStatus(10L, 8L, request, principal);

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.IN_PROGRESS);
        verify(workflowTransitionPolicy).requireAllowedTransition(
                10L, com.lightalm.domain.TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", principal);
    }

    /** ADR-012 §D 회귀 테스트: 워크플로우 정책이 거부하면 changeStatus 전체가 실패해야 한다. */
    @Test
    void changeStatus_whenWorkflowPolicyRejects_propagatesException() {
        Requirement existing = Requirement.builder().id(11L).project(project).reqKey("LALM-R11")
                .status(RequirementStatus.APPROVED).build();
        when(requirementRepository.findById(11L)).thenReturn(Optional.of(existing));
        ChangeRequirementStatusRequest request = new ChangeRequirementStatusRequest();
        request.setStatus(RequirementStatus.IN_PROGRESS);
        org.mockito.Mockito.doThrow(new ValidationException("전이가 허용되지 않습니다."))
                .when(workflowTransitionPolicy)
                .requireAllowedTransition(10L, com.lightalm.domain.TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", principal);

        assertThatThrownBy(() -> requirementService.changeStatus(10L, 11L, request, principal))
                .isInstanceOf(ValidationException.class);
        assertThat(existing.getStatus()).isEqualTo(RequirementStatus.APPROVED);
    }

    @Test
    void changeStatus_rejectsDirectDraftToApprovedTransition() {
        Requirement existing = Requirement.builder().id(9L).project(project).reqKey("LALM-R9")
                .status(RequirementStatus.DRAFT).build();
        when(requirementRepository.findById(9L)).thenReturn(Optional.of(existing));
        ChangeRequirementStatusRequest request = new ChangeRequirementStatusRequest();
        request.setStatus(RequirementStatus.APPROVED);

        assertThatThrownBy(() -> requirementService.changeStatus(10L, 9L, request, principal))
                .isInstanceOf(ValidationException.class);
    }
}
