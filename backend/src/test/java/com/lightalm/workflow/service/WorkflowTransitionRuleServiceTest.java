package com.lightalm.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import com.lightalm.workflow.dto.CreateWorkflowTransitionRuleRequest;
import com.lightalm.workflow.dto.NextStatusesResponse;
import com.lightalm.workflow.dto.WorkflowRuleListResponse;
import com.lightalm.workflow.dto.WorkflowTransitionRuleResponse;
import com.lightalm.workflow.repository.WorkflowTransitionRuleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-012 §D.4. 설정화면 CRUD(PROJECT_ADMIN+)의 권한/입력 검증과, 규칙 전부 삭제 시
 * 자유 전이 모드로 복귀하는지를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowTransitionRuleServiceTest {

    @Mock
    private WorkflowTransitionRuleRepository ruleRepository;
    @Mock
    private WorkflowTransitionPolicy workflowTransitionPolicy;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkflowTransitionRuleService ruleService;

    private UserPrincipal principal;
    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("admin1").password("hash")
                .email("admin1@example.com").fullName("Admin One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
    }

    @Test
    void list_whenNoRulesExist_reportsFreeTransitionMode() {
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of());

        WorkflowRuleListResponse response = ruleService.list(10L, TargetType.REQUIREMENT, principal);

        assertThat(response.freeTransitionMode()).isTrue();
        assertThat(response.rules()).isEmpty();
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
    }

    @Test
    void list_rejectsTestCaseTargetType() {
        assertThatThrownBy(() -> ruleService.list(10L, TargetType.TEST_CASE, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withValidStatusPair_savesRule() {
        when(ruleRepository.existsByProjectIdAndTargetTypeAndFromStatusAndToStatus(
                10L, TargetType.REQUIREMENT, "DRAFT", "REJECTED")).thenReturn(false);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(ruleRepository.save(any(WorkflowTransitionRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateWorkflowTransitionRuleRequest request =
                new CreateWorkflowTransitionRuleRequest(TargetType.REQUIREMENT, "DRAFT", "REJECTED", null);

        WorkflowTransitionRuleResponse response = ruleService.create(10L, request, principal);

        assertThat(response.fromStatus()).isEqualTo("DRAFT");
        assertThat(response.toStatus()).isEqualTo("REJECTED");
        assertThat(response.allowedRole()).isNull();
    }

    @Test
    void create_rejectsTestCaseTargetType() {
        CreateWorkflowTransitionRuleRequest request =
                new CreateWorkflowTransitionRuleRequest(TargetType.TEST_CASE, "DRAFT", "READY", null);

        assertThatThrownBy(() -> ruleService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsUnknownStatusValue() {
        CreateWorkflowTransitionRuleRequest request =
                new CreateWorkflowTransitionRuleRequest(TargetType.REQUIREMENT, "DRAFT", "NOT_A_STATUS", null);

        assertThatThrownBy(() -> ruleService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsSameFromAndToStatus() {
        CreateWorkflowTransitionRuleRequest request =
                new CreateWorkflowTransitionRuleRequest(TargetType.REQUIREMENT, "DRAFT", "DRAFT", null);

        assertThatThrownBy(() -> ruleService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsDuplicateRule() {
        when(ruleRepository.existsByProjectIdAndTargetTypeAndFromStatusAndToStatus(
                10L, TargetType.REQUIREMENT, "DRAFT", "REJECTED")).thenReturn(true);

        CreateWorkflowTransitionRuleRequest request =
                new CreateWorkflowTransitionRuleRequest(TargetType.REQUIREMENT, "DRAFT", "REJECTED", null);

        assertThatThrownBy(() -> ruleService.create(10L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void delete_removesRule() {
        WorkflowTransitionRule rule = WorkflowTransitionRule.builder()
                .project(project).targetType(TargetType.REQUIREMENT).fromStatus("DRAFT").toStatus("REJECTED").build();
        when(ruleRepository.findByIdAndProjectId(5L, 10L)).thenReturn(Optional.of(rule));

        ruleService.delete(10L, 5L, principal);

        verify(ruleRepository).delete(rule);
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
    }

    @Test
    void nextStatuses_requiresViewerRoleAndDelegatesToPolicy() {
        when(workflowTransitionPolicy.nextStatuses(10L, TargetType.REQUIREMENT, "DRAFT"))
                .thenReturn(new NextStatusesResponse(true, List.of("APPROVED", "REJECTED")));

        NextStatusesResponse response = ruleService.nextStatuses(10L, TargetType.REQUIREMENT, "DRAFT", principal);

        assertThat(response.freeTransitionMode()).isTrue();
        assertThat(response.nextStatuses()).containsExactly("APPROVED", "REJECTED");
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.VIEWER);
    }
}
