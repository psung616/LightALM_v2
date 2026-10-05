package com.lightalm.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import com.lightalm.workflow.dto.NextStatusesResponse;
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
 * ADR-012 §D.3. 가장 중요한 회귀 시나리오(규칙 미등록 시 자유 전이)부터 화이트리스트 모드,
 * allowedRole 검사, 시스템 ADMIN/PROJECT_ADMIN의 lock-out 방지 예외까지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowTransitionPolicyTest {

    @Mock
    private WorkflowTransitionRuleRepository ruleRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private WorkflowTransitionPolicy policy;

    private Project project;
    private UserPrincipal memberPrincipal;
    private UserPrincipal systemAdminPrincipal;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();

        User member = User.builder().id(1L).username("member1").password("hash")
                .email("member1@example.com").fullName("Member One").systemRole(SystemRole.USER).enabled(true).build();
        memberPrincipal = new UserPrincipal(member);

        User admin = User.builder().id(2L).username("admin1").password("hash")
                .email("admin1@example.com").fullName("Admin One").systemRole(SystemRole.ADMIN).enabled(true).build();
        systemAdminPrincipal = new UserPrincipal(admin);
    }

    @Test
    void requireAllowedTransition_whenNoRulesRegistered_allowsAnyTransition() {
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of());

        policy.requireAllowedTransition(10L, TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", memberPrincipal);
        // no exception => 자유 전이 모드 유지(하위 호환 핵심 회귀)
    }

    @Test
    void requireAllowedTransition_whenFromEqualsTo_isNoOpRegardlessOfRules() {
        policy.requireAllowedTransition(10L, TargetType.REQUIREMENT, "APPROVED", "APPROVED", memberPrincipal);
        // 규칙 조회 자체가 필요 없는 조기 반환 경로
    }

    @Test
    void requireAllowedTransition_whenRuleExistsForPair_allowsMatchingTransition() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));
        when(projectMemberService.findRole(10L, 1L)).thenReturn(Optional.of(ProjectRole.MEMBER));

        policy.requireAllowedTransition(10L, TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", memberPrincipal);
    }

    @Test
    void requireAllowedTransition_whenWhitelistModeAndPairNotRegistered_rejects() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));
        when(projectMemberService.findRole(10L, 1L)).thenReturn(Optional.of(ProjectRole.MEMBER));

        assertThatThrownBy(() -> policy.requireAllowedTransition(
                10L, TargetType.REQUIREMENT, "IMPLEMENTED", "VERIFIED", memberPrincipal))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void requireAllowedTransition_whenAllowedRoleRequiresProjectAdminAndActorIsMember_rejects() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", ProjectRole.PROJECT_ADMIN);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));
        when(projectMemberService.findRole(10L, 1L)).thenReturn(Optional.of(ProjectRole.MEMBER));

        assertThatThrownBy(() -> policy.requireAllowedTransition(
                10L, TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", memberPrincipal))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireAllowedTransition_whenActorIsSystemAdmin_bypassesWhitelistEntirely() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));

        policy.requireAllowedTransition(10L, TargetType.REQUIREMENT, "IMPLEMENTED", "VERIFIED", systemAdminPrincipal);
        // 등록되지 않은 전이인데도 시스템 ADMIN이라 예외 없이 통과해야 한다(lock-out 방지)
    }

    @Test
    void requireAllowedTransition_whenActorIsProjectAdmin_bypassesWhitelistEntirely() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));
        when(projectMemberService.findRole(10L, 1L)).thenReturn(Optional.of(ProjectRole.PROJECT_ADMIN));

        policy.requireAllowedTransition(10L, TargetType.REQUIREMENT, "IMPLEMENTED", "VERIFIED", memberPrincipal);
        // 등록되지 않은 전이인데도 그 프로젝트의 PROJECT_ADMIN이라 예외 없이 통과해야 한다
    }

    /**
     * qa-tester 2026-10-05 발견 버그의 회귀 테스트. {@code requireRegisteredTransition}은
     * {@code requireAllowedTransition}과 달리 actor/role 파라미터 자체가 없어 PROJECT_ADMIN/
     * 시스템 ADMIN lock-out 우회가 구조적으로 불가능해야 한다 — {@code ApprovalService.decide()}는
     * 호출자를 이미 PROJECT_ADMIN 이상으로 제한하므로, 우회 로직이 하나라도 남아있으면 이 메서드는
     * 실제 호출 경로에서 절대 거부를 발생시킬 수 없는 죽은 코드가 된다.
     */
    @Test
    void requireRegisteredTransition_whenNoRulesRegistered_allowsAnyTransition() {
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of());

        policy.requireRegisteredTransition(10L, TargetType.REQUIREMENT, "DRAFT", "APPROVED");
    }

    @Test
    void requireRegisteredTransition_whenFromEqualsTo_isNoOp() {
        policy.requireRegisteredTransition(10L, TargetType.REQUIREMENT, "DRAFT", "DRAFT");
    }

    @Test
    void requireRegisteredTransition_whenPairIsRegistered_allows() {
        WorkflowTransitionRule rule = buildRule(TargetType.REQUIREMENT, "DRAFT", "APPROVED", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(rule));

        policy.requireRegisteredTransition(10L, TargetType.REQUIREMENT, "DRAFT", "APPROVED");
    }

    @Test
    void requireRegisteredTransition_whenWhitelistModeAndPairNotRegistered_rejectsWithNoBypassPossible() {
        // DRAFT->APPROVED는 등록하지 않고, 같은 target_type에 다른 전이 하나만 등록 — 재현 시나리오 그대로.
        WorkflowTransitionRule unrelatedRule = buildRule(TargetType.REQUIREMENT, "APPROVED", "IN_PROGRESS", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.REQUIREMENT)).thenReturn(List.of(unrelatedRule));

        assertThatThrownBy(() -> policy.requireRegisteredTransition(10L, TargetType.REQUIREMENT, "DRAFT", "APPROVED"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nextStatuses_whenNoRulesRegistered_returnsFreeModeWithAllOtherStatuses() {
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.ISSUE)).thenReturn(List.of());

        NextStatusesResponse response = policy.nextStatuses(10L, TargetType.ISSUE, "TODO");

        assertThat(response.freeTransitionMode()).isTrue();
        assertThat(response.nextStatuses()).containsExactlyInAnyOrder("IN_PROGRESS", "IN_REVIEW", "DONE", "CLOSED");
    }

    @Test
    void nextStatuses_whenRulesRegistered_returnsOnlyMatchingToStatuses() {
        WorkflowTransitionRule rule1 = buildRule(TargetType.ISSUE, "TODO", "IN_PROGRESS", null);
        WorkflowTransitionRule rule2 = buildRule(TargetType.ISSUE, "IN_PROGRESS", "DONE", null);
        when(ruleRepository.findByProjectIdAndTargetType(10L, TargetType.ISSUE)).thenReturn(List.of(rule1, rule2));

        NextStatusesResponse response = policy.nextStatuses(10L, TargetType.ISSUE, "TODO");

        assertThat(response.freeTransitionMode()).isFalse();
        assertThat(response.nextStatuses()).containsExactly("IN_PROGRESS");
    }

    private WorkflowTransitionRule buildRule(TargetType targetType, String from, String to, ProjectRole allowedRole) {
        return WorkflowTransitionRule.builder()
                .project(project)
                .targetType(targetType)
                .fromStatus(from)
                .toStatus(to)
                .allowedRole(allowedRole)
                .build();
    }
}
