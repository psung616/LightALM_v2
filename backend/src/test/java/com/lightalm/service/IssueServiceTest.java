package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangeIssueStatusRequest;
import com.lightalm.dto.CreateIssueRequest;
import com.lightalm.dto.IssueResponse;
import com.lightalm.enumeration.service.EnumerationValueValidator;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.workflow.service.WorkflowTransitionPolicy;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    private IssueRepository issueRepository;
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
    private IssueService issueService;

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
    void create_generatesKeyAndPersistsIssue() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle("Login API bug");
        request.setType(IssueType.BUG);

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectService.nextIssueKey(10L)).thenReturn("LALM-101");
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
            Issue issue = invocation.getArgument(0);
            issue.setId(100L);
            issue.setCreatedAt(LocalDateTime.now());
            issue.setUpdatedAt(LocalDateTime.now());
            return issue;
        });

        IssueResponse response = issueService.create(10L, request, principal);

        assertThat(response.getIssueKey()).isEqualTo("LALM-101");
        assertThat(response.getStatus()).isEqualTo(IssueStatus.TODO);
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM.name());
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.MEMBER);
        verify(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "MEDIUM");
    }

    @Test
    void create_whenEnumerationValidatorRejectsPriority_propagatesException() {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle("Login API bug");
        request.setType(IssueType.BUG);
        request.setPriority("BLOCKER");

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new com.lightalm.exception.ValidationException("유효하지 않은 PRIORITY 값입니다: BLOCKER"))
                .when(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "BLOCKER");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> issueService.create(10L, request, principal))
                .isInstanceOf(com.lightalm.exception.ValidationException.class);
        verify(issueRepository, org.mockito.Mockito.never()).save(any(Issue.class));
    }

    /** qa-tester 반려(2026-10-05): priority를 안 바꾸는 수정은 기존 값(폐기된 BLOCKER 포함)을 기존→신규 비교 경로로 넘긴다. */
    @Test
    void update_withoutPriorityChange_validatesAsUnchangedValue() {
        Issue existing = Issue.builder().id(13L).project(project).issueKey("LALM-13").title("blk iss")
                .type(IssueType.BUG).priority("BLOCKER").build();
        when(issueRepository.findById(13L)).thenReturn(Optional.of(existing));
        com.lightalm.dto.UpdateIssueRequest request = new com.lightalm.dto.UpdateIssueRequest();
        request.setTitle("blk iss renamed");
        request.setType(IssueType.BUG);

        IssueResponse response = issueService.update(10L, 13L, request, principal);

        assertThat(response.getTitle()).isEqualTo("blk iss renamed");
        assertThat(response.getPriority()).isEqualTo("BLOCKER");
        verify(enumerationValueValidator).requireValidValueForChange(10L, "PRIORITY", "BLOCKER", "BLOCKER");
    }

    @Test
    void changeStatus_toDone_setsResolvedAt() {
        Issue existing = Issue.builder().id(5L).project(project).issueKey("LALM-105").status(IssueStatus.IN_PROGRESS).build();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(existing));
        ChangeIssueStatusRequest request = new ChangeIssueStatusRequest();
        request.setStatus(IssueStatus.DONE);

        IssueResponse response = issueService.changeStatus(10L, 5L, request, principal);

        assertThat(response.getStatus()).isEqualTo(IssueStatus.DONE);
        assertThat(response.getResolvedAt()).isNotNull();
    }

    @Test
    void changeStatus_awayFromDone_clearsResolvedAt() {
        Issue existing = Issue.builder().id(6L).project(project).issueKey("LALM-106")
                .status(IssueStatus.DONE).resolvedAt(LocalDateTime.now()).build();
        when(issueRepository.findById(6L)).thenReturn(Optional.of(existing));
        ChangeIssueStatusRequest request = new ChangeIssueStatusRequest();
        request.setStatus(IssueStatus.IN_REVIEW);

        IssueResponse response = issueService.changeStatus(10L, 6L, request, principal);

        assertThat(response.getStatus()).isEqualTo(IssueStatus.IN_REVIEW);
        assertThat(response.getResolvedAt()).isNull();
    }

    /** ADR-012 §D 회귀 테스트: 워크플로우 정책이 거부하면 changeStatus 전체가 실패해야 한다. */
    @Test
    void changeStatus_whenWorkflowPolicyRejects_propagatesException() {
        Issue existing = Issue.builder().id(8L).project(project).issueKey("LALM-108").status(IssueStatus.TODO).build();
        when(issueRepository.findById(8L)).thenReturn(Optional.of(existing));
        ChangeIssueStatusRequest request = new ChangeIssueStatusRequest();
        request.setStatus(IssueStatus.DONE);
        org.mockito.Mockito.doThrow(new com.lightalm.exception.ValidationException("전이가 허용되지 않습니다."))
                .when(workflowTransitionPolicy)
                .requireAllowedTransition(10L, com.lightalm.domain.TargetType.ISSUE, "TODO", "DONE", principal);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> issueService.changeStatus(10L, 8L, request, principal))
                .isInstanceOf(com.lightalm.exception.ValidationException.class);
        assertThat(existing.getStatus()).isEqualTo(IssueStatus.TODO);
    }

    @Test
    void delete_requiresProjectAdminRole() {
        Issue existing = Issue.builder().id(7L).project(project).issueKey("LALM-107").build();
        when(issueRepository.findById(7L)).thenReturn(Optional.of(existing));

        issueService.delete(10L, 7L, principal);

        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
        verify(issueRepository).delete(existing);
    }
}
