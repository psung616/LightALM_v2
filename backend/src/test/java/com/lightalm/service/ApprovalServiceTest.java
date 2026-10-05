package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.ApprovalDecision;
import com.lightalm.domain.ApprovalRequest;
import com.lightalm.domain.ApprovalStatus;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.ApprovalDecisionRequest;
import com.lightalm.dto.ApprovalRequestResponse;
import com.lightalm.dto.CreateApprovalRequestRequest;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.ApprovalRequestRepository;
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
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;
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
    private WorkflowTransitionPolicy workflowTransitionPolicy;

    @InjectMocks
    private ApprovalService approvalService;

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
    void create_rejectsWhenRequirementNotDraft() {
        Requirement requirement = Requirement.builder().id(5L).project(project).reqKey("LALM-R5")
                .title("로그인 기능").status(RequirementStatus.APPROVED).build();
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(requirement));

        CreateApprovalRequestRequest request = new CreateApprovalRequestRequest();
        request.setRequestedStatus(RequirementStatus.APPROVED);

        assertThatThrownBy(() -> approvalService.create(10L, 5L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsDuplicatePendingRequest() {
        Requirement requirement = Requirement.builder().id(5L).project(project).reqKey("LALM-R5")
                .title("로그인 기능").status(RequirementStatus.DRAFT).build();
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(requirement));
        when(approvalRequestRepository.existsByTargetTypeAndTargetIdAndStatus(TargetType.REQUIREMENT, 5L, ApprovalStatus.PENDING))
                .thenReturn(true);

        CreateApprovalRequestRequest request = new CreateApprovalRequestRequest();
        request.setRequestedStatus(RequirementStatus.APPROVED);

        assertThatThrownBy(() -> approvalService.create(10L, 5L, request, principal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void decide_approve_setsRequirementAndApprovalStatusToApproved() {
        Requirement requirement = Requirement.builder().id(5L).project(project).reqKey("LALM-R5")
                .title("로그인 기능").status(RequirementStatus.DRAFT).build();
        ApprovalRequest approval = ApprovalRequest.builder().id(20L).project(project)
                .targetType(TargetType.REQUIREMENT).targetId(5L)
                .requestedStatus(RequirementStatus.APPROVED).status(ApprovalStatus.PENDING).build();
        when(approvalRequestRepository.findById(20L)).thenReturn(Optional.of(approval));
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(requirement));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApprovalDecisionRequest request = new ApprovalDecisionRequest();
        request.setDecision(ApprovalDecision.APPROVE);
        request.setComment("확인했습니다");

        ApprovalRequestResponse response = approvalService.decide(10L, 20L, request, principal);

        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.APPROVED);
        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(response.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        org.mockito.Mockito.verify(workflowTransitionPolicy).requireRegisteredTransition(
                10L, TargetType.REQUIREMENT, "DRAFT", "APPROVED");
    }

    /**
     * ADR-012 §D.3 AND 조건: 승인은 APPROVE로 결정됐지만 워크플로우 화이트리스트가 이 전이를
     * 막으면, 요구사항 상태/승인 상태 둘 다 변경되지 않고 전체 decide() 호출이 실패해야 한다.
     */
    @Test
    void decide_approve_whenWorkflowPolicyRejects_leavesRequirementAndApprovalUnchanged() {
        Requirement requirement = Requirement.builder().id(5L).project(project).reqKey("LALM-R5")
                .title("로그인 기능").status(RequirementStatus.DRAFT).build();
        ApprovalRequest approval = ApprovalRequest.builder().id(20L).project(project)
                .targetType(TargetType.REQUIREMENT).targetId(5L)
                .requestedStatus(RequirementStatus.APPROVED).status(ApprovalStatus.PENDING).build();
        when(approvalRequestRepository.findById(20L)).thenReturn(Optional.of(approval));
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(requirement));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new ValidationException("전이가 허용되지 않습니다."))
                .when(workflowTransitionPolicy)
                .requireRegisteredTransition(10L, TargetType.REQUIREMENT, "DRAFT", "APPROVED");

        ApprovalDecisionRequest request = new ApprovalDecisionRequest();
        request.setDecision(ApprovalDecision.APPROVE);
        request.setComment("확인했습니다");

        assertThatThrownBy(() -> approvalService.decide(10L, 20L, request, principal))
                .isInstanceOf(ValidationException.class);
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.DRAFT);
        assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }
}
