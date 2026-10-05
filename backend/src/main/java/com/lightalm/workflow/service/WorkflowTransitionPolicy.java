package com.lightalm.workflow.service;

import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.TargetType;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.workflow.domain.WorkflowTransitionRule;
import com.lightalm.workflow.dto.NextStatusesResponse;
import com.lightalm.workflow.repository.WorkflowTransitionRuleRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-012 §D.3. "상태 A→B 허용 여부(참/거짓) + 최소 역할"만 표현하는 화이트리스트 매트릭스
 * 적용 로직이다. <b>범용 워크플로우 엔진이 아니다</b> — 조건 분기, 전이 시 자동 액션, 승인자
 * 체인은 다루지 않는다(그런 요소는 이미 §1.2 승인 워크플로우/리뷰 사이클이 각자 좁은 범위에서
 * 다룬다).
 *
 * <p><b>보수적 기본값(하위 호환의 핵심)</b>: 프로젝트가 특정 target_type에 대해 규칙을 하나도
 * 등록하지 않았으면 모든 상태 간 자유 전이를 그대로 허용한다 — 이 기능을 쓰지 않는 프로젝트는
 * 100% 기존과 동일하게 동작한다. 규칙을 하나 이상 등록하면 그 프로젝트의 그 target_type은
 * 화이트리스트 모드로 전환되어 등록된 (from,to) 조합만 허용한다(기본 거부).</p>
 *
 * <p>시스템 {@code ADMIN}과 해당 프로젝트의 {@code PROJECT_ADMIN}은 화이트리스트 규칙과
 * 무관하게 항상 모든 전이가 허용된다 — 관리자가 스스로 설정한 규칙 때문에 잠기는 상황
 * (lock-out)을 방지하기 위한 명시적 예외다(ADR-011의 라이센스 ADMIN 로그인 예외와 동일한
 * 설계 사유).</p>
 */
@Service
@RequiredArgsConstructor
public class WorkflowTransitionPolicy {

    private final WorkflowTransitionRuleRepository ruleRepository;
    private final ProjectMemberService projectMemberService;

    /**
     * 기존 {@code PATCH .../status} 처리 경로(요구사항/이슈)에 거는 훅. 규칙 미등록 시 아무
     * 일도 하지 않는다(자유 전이, 하위 호환). 시스템 {@code ADMIN}/해당 프로젝트의
     * {@code PROJECT_ADMIN}은 lock-out 방지를 위해 화이트리스트와 무관하게 항상 통과한다.
     *
     * <p><b>승인 워크플로우의 {@code DRAFT→APPROVED} 실행 지점({@code ApprovalService.decide()})에는
     * 이 메서드를 쓰지 않는다</b> — {@link #requireRegisteredTransition} 참고(qa-tester
     * 2026-10-05 발견: lock-out 우회 때문에 이 메서드를 그 지점에 쓰면 체크가 무력화된다).</p>
     */
    @Transactional(readOnly = true)
    public void requireAllowedTransition(Long projectId, TargetType targetType, String fromStatus, String toStatus,
                                          UserPrincipal actor) {
        if (fromStatus.equals(toStatus)) {
            return;
        }

        List<WorkflowTransitionRule> rules = ruleRepository.findByProjectIdAndTargetType(projectId, targetType);
        if (rules.isEmpty()) {
            return;
        }

        if (actor.isAdmin()) {
            return;
        }
        ProjectRole actorRole = projectMemberService.findRole(projectId, actor.getId())
                .orElseThrow(() -> new ForbiddenException("해당 프로젝트에 대한 접근 권한이 없습니다."));
        if (actorRole == ProjectRole.PROJECT_ADMIN) {
            return;
        }

        WorkflowTransitionRule matched = rules.stream()
                .filter(rule -> rule.getFromStatus().equals(fromStatus) && rule.getToStatus().equals(toStatus))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "이 프로젝트는 워크플로우 규칙이 설정되어 있어 " + fromStatus + " → " + toStatus + " 전이는 허용되지 않습니다."));

        ProjectRole requiredRole = matched.getAllowedRole() != null ? matched.getAllowedRole() : ProjectRole.MEMBER;
        if (!actorRole.isAtLeast(requiredRole)) {
            throw new ForbiddenException("이 전이를 수행하려면 " + requiredRole + " 이상의 권한이 필요합니다.");
        }
    }

    /**
     * 승인 워크플로우의 {@code DRAFT→APPROVED} 전이 실행 지점({@code ApprovalService.decide()})
     * 전용 변형이다 — {@link #requireAllowedTransition}과 달리 시스템 {@code ADMIN}/
     * {@code PROJECT_ADMIN}에 대한 lock-out 방지 우회를 적용하지 않는다.
     *
     * <p>{@code decide()}는 호출 전에 이미 {@code ProjectMemberService.requireRole(projectId,
     * principal, PROJECT_ADMIN)}으로 호출자를 PROJECT_ADMIN 이상으로 제한한다. {@code
     * ProjectRole}에는 PROJECT_ADMIN보다 높은 역할이 없으므로, {@link #requireAllowedTransition}의
     * "시스템 ADMIN/PROJECT_ADMIN은 항상 통과" 우회를 그대로 쓰면 이 호출 경로에 도달할 수 있는
     * 모든 실제 호출자가 예외 없이 우회 조건에 걸려 화이트리스트 체크가 전혀 작동하지 않는다
     * (qa-tester 2026-10-05 실제 Postgres+API 검증에서 발견한 버그 — Mockito로 정책을 완전히
     * 모킹한 단위 테스트는 이 상호작용을 검증하지 못했다).</p>
     *
     * <p>이 호출 지점에는 애초에 lock-out 시나리오가 없다 — 승인이 거부되어도 요구사항은
     * 그냥 DRAFT로 남을 뿐이고, PROJECT_ADMIN은 {@code config/workflow-rules} 설정 API로
     * 스스로 규칙을 추가해 막힌 상태를 풀 수 있는 별도 경로가 이미 있다. 따라서 role 검사 없이
     * 단순히 "자유 전이 모드인가, 아니면 이 (from,to) 조합이 화이트리스트에 등록돼 있는가"만
     * 확인한다 — 등록돼 있다면 allowedRole과 무관하게 통과시킨다(호출자가 이미 PROJECT_ADMIN
     * 이상임을 {@code decide()}가 보장했으므로, 등록된 규칙의 allowedRole이 무엇이든 항상
     * 충족된다).</p>
     */
    @Transactional(readOnly = true)
    public void requireRegisteredTransition(Long projectId, TargetType targetType, String fromStatus, String toStatus) {
        if (fromStatus.equals(toStatus)) {
            return;
        }
        List<WorkflowTransitionRule> rules = ruleRepository.findByProjectIdAndTargetType(projectId, targetType);
        if (rules.isEmpty()) {
            return;
        }
        boolean matched = rules.stream()
                .anyMatch(rule -> rule.getFromStatus().equals(fromStatus) && rule.getToStatus().equals(toStatus));
        if (!matched) {
            throw new ValidationException(
                    "이 프로젝트는 워크플로우 규칙이 설정되어 있어 " + fromStatus + " → " + toStatus + " 전이는 허용되지 않습니다.");
        }
    }

    /**
     * 화면의 "상태 변경" 드롭다운/§5.5 Workflow 차트 구성용 — fromStatus에서 전이 가능한 다음
     * 상태 목록과 자유 전이 모드 여부를 함께 돌려준다. 권한 검사는 호출자(서비스 레이어)의
     * 책임이다.
     */
    @Transactional(readOnly = true)
    public NextStatusesResponse nextStatuses(Long projectId, TargetType targetType, String fromStatus) {
        List<WorkflowTransitionRule> rules = ruleRepository.findByProjectIdAndTargetType(projectId, targetType);
        if (rules.isEmpty()) {
            return new NextStatusesResponse(true, allStatusesExcept(targetType, fromStatus));
        }
        List<String> toStatuses = rules.stream()
                .filter(rule -> rule.getFromStatus().equals(fromStatus))
                .map(WorkflowTransitionRule::getToStatus)
                .toList();
        return new NextStatusesResponse(false, toStatuses);
    }

    private List<String> allStatusesExcept(TargetType targetType, String fromStatus) {
        return switch (targetType) {
            case REQUIREMENT -> Arrays.stream(RequirementStatus.values())
                    .map(Enum::name)
                    .filter(status -> !status.equals(fromStatus))
                    .toList();
            case ISSUE -> Arrays.stream(IssueStatus.values())
                    .map(Enum::name)
                    .filter(status -> !status.equals(fromStatus))
                    .toList();
            case TEST_CASE -> List.of();
        };
    }
}
