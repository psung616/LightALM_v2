package com.lightalm.baseline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.baseline.domain.Baseline;
import com.lightalm.baseline.domain.BaselineItem;
import com.lightalm.baseline.dto.BaselineDiffResponse;
import com.lightalm.baseline.dto.BaselineFieldChange;
import com.lightalm.baseline.dto.BaselineFieldChangeKind;
import com.lightalm.baseline.dto.BaselineItemChangeType;
import com.lightalm.baseline.dto.BaselineItemDiff;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ADR-008(Phase 16) / 08-dev-phases.md Phase 16 DoD "베이스라인 생성 후 원본 요구사항을 수정하고 diff API를
 * 호출해 변경분이 정확히 반환되는지". {@link BaselineSnapshotFactory}·{@link BaselineSnapshotJsonCodec}은 실제
 * 인스턴스(원본 Repository만 mock)를 써서 "스냅샷 캡처 → JSON 직렬화 → 현재 값과 필드 비교" 전체 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BaselineDiffServiceTest {

    @Mock
    private BaselineRepository baselineRepository;
    @Mock
    private BaselineItemRepository baselineItemRepository;
    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BaselineSnapshotFactory snapshotFactory;
    private BaselineDiffService baselineDiffService;

    private Project project;
    private Project otherProject;
    private UserPrincipal principal;
    private Requirement requirement;
    private Baseline baseline;

    @BeforeEach
    void setUp() {
        snapshotFactory = new BaselineSnapshotFactory(requirementRepository, issueRepository, testCaseRepository);
        baselineDiffService = new BaselineDiffService(baselineRepository, baselineItemRepository, snapshotFactory,
                new BaselineSnapshotJsonCodec(objectMapper), projectMemberService);

        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        otherProject = Project.builder().id(20L).projectKey("OTHER").name("Other").build();
        User admin = User.builder().id(1L).username("padmin").password("hash").email("padmin@example.com")
                .fullName("PA").systemRole(SystemRole.USER).enabled(true).build();
        User assignee = User.builder().id(7L).username("dev").password("hash").email("dev@example.com")
                .fullName("Dev").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(admin);
        requirement = Requirement.builder().id(100L).project(project).reqKey("LALM-REQ-1").title("로그인 기능")
                .description(null).type(RequirementType.FUNCTIONAL).priority("HIGH").status(RequirementStatus.DRAFT)
                .assignedTo(assignee).dueDate(LocalDate.of(2026, 12, 31)).build();

        baseline = Baseline.builder().project(project).name("v1.0 기준선").description("릴리스 전").createdBy(admin).build();
        ReflectionTestUtils.setField(baseline, "id", 300L);
    }

    /** 현재 상태를 실제 생성 경로와 동일하게(팩토리 → ObjectMapper) JSON 스냅샷으로 얼린다. */
    private BaselineItem freeze(TargetType type, Long targetId) throws Exception {
        Map<String, Object> current = snapshotFactory.captureCurrent(10L, type, targetId).orElseThrow();
        BaselineItem item = BaselineItem.builder().baseline(baseline).targetType(type).targetId(targetId)
                .snapshot(objectMapper.writeValueAsString(current)).build();
        ReflectionTestUtils.setField(item, "id", 900L + targetId);
        return item;
    }

    private void givenBaselineWithItems(BaselineItem... items) {
        when(baselineRepository.findByIdAndProjectId(300L, 10L)).thenReturn(Optional.of(baseline));
        when(baselineItemRepository.findByBaselineIdOrderById(300L)).thenReturn(List.of(items));
    }

    private static Map<String, BaselineFieldChange> byField(BaselineItemDiff diff) {
        return diff.changes().stream().collect(Collectors.toMap(BaselineFieldChange::field, c -> c));
    }

    @Test
    void unchangedTarget_reportsNoChanges() throws Exception {
        when(requirementRepository.findById(100L)).thenReturn(Optional.of(requirement));
        givenBaselineWithItems(freeze(TargetType.REQUIREMENT, 100L));

        BaselineDiffResponse diff = baselineDiffService.diffAgainstCurrent(10L, 300L, principal);

        // assignedToId(Long)가 JSON 왕복 후 Integer로 읽혀도 "변경"으로 잡히지 않아야 한다.
        assertThat(diff.items()).singleElement().satisfies(d -> {
            assertThat(d.changeType()).isEqualTo(BaselineItemChangeType.UNCHANGED);
            assertThat(d.changes()).isEmpty();
            assertThat(d.key()).isEqualTo("LALM-REQ-1");
        });
        assertThat(diff.unchangedCount()).isEqualTo(1);
        assertThat(diff.modifiedCount()).isZero();
        assertThat(diff.deletedCount()).isZero();
    }

    @Test
    void afterRequirementEdited_returnsExactFieldChanges() throws Exception {
        when(requirementRepository.findById(100L)).thenReturn(Optional.of(requirement));
        givenBaselineWithItems(freeze(TargetType.REQUIREMENT, 100L));

        // 베이스라인 생성 후 원본 요구사항 수정
        requirement.setTitle("로그인 기능(OTP 포함)");
        requirement.setStatus(RequirementStatus.APPROVED);
        requirement.setDescription("OTP 2단계 인증 추가");
        requirement.setAssignedTo(null);

        BaselineDiffResponse diff = baselineDiffService.diffAgainstCurrent(10L, 300L, principal);

        BaselineItemDiff itemDiff = diff.items().get(0);
        assertThat(itemDiff.changeType()).isEqualTo(BaselineItemChangeType.MODIFIED);
        assertThat(itemDiff.title()).isEqualTo("로그인 기능(OTP 포함)");
        Map<String, BaselineFieldChange> changes = byField(itemDiff);
        assertThat(changes.keySet()).containsExactlyInAnyOrder("title", "status", "description", "assignedToId");

        assertThat(changes.get("title").changeKind()).isEqualTo(BaselineFieldChangeKind.MODIFIED);
        assertThat(changes.get("title").before().asText()).isEqualTo("로그인 기능");
        assertThat(changes.get("title").after().asText()).isEqualTo("로그인 기능(OTP 포함)");
        assertThat(changes.get("status").before().asText()).isEqualTo("DRAFT");
        assertThat(changes.get("status").after().asText()).isEqualTo("APPROVED");
        assertThat(changes.get("description").changeKind()).isEqualTo(BaselineFieldChangeKind.ADDED);
        assertThat(changes.get("description").before()).isNull();
        assertThat(changes.get("assignedToId").changeKind()).isEqualTo(BaselineFieldChangeKind.REMOVED);
        assertThat(changes.get("assignedToId").before().asLong()).isEqualTo(7L);
        assertThat(changes.get("assignedToId").after()).isNull();
        assertThat(diff.modifiedCount()).isEqualTo(1);
    }

    @Test
    void deletedTarget_isReportedAsDeletedWithSnapshotIdentity() throws Exception {
        when(requirementRepository.findById(100L)).thenReturn(Optional.of(requirement), Optional.empty());
        givenBaselineWithItems(freeze(TargetType.REQUIREMENT, 100L));

        BaselineDiffResponse diff = baselineDiffService.diffAgainstCurrent(10L, 300L, principal);

        BaselineItemDiff itemDiff = diff.items().get(0);
        assertThat(itemDiff.changeType()).isEqualTo(BaselineItemChangeType.DELETED);
        assertThat(itemDiff.key()).isEqualTo("LALM-REQ-1");
        assertThat(itemDiff.title()).isEqualTo("로그인 기능");
        assertThat(itemDiff.changes()).isNotEmpty()
                .allSatisfy(c -> {
                    assertThat(c.changeKind()).isEqualTo(BaselineFieldChangeKind.REMOVED);
                    assertThat(c.after()).isNull();
                });
        // 스냅샷 시점에 null이었던 description은 "삭제된 값"으로 나오지 않는다.
        assertThat(byField(itemDiff)).doesNotContainKey("description");
        assertThat(diff.deletedCount()).isEqualTo(1);
    }

    @Test
    void targetMovedOutOfProject_isTreatedAsDeleted() throws Exception {
        Issue issue = Issue.builder().id(200L).project(project).issueKey("LALM-1").title("버그")
                .type(IssueType.BUG).priority("LOW").status(IssueStatus.TODO).build();
        Issue movedIssue = Issue.builder().id(200L).project(otherProject).issueKey("LALM-1").title("버그")
                .type(IssueType.BUG).priority("LOW").status(IssueStatus.TODO).build();
        when(issueRepository.findById(200L)).thenReturn(Optional.of(issue), Optional.of(movedIssue));
        givenBaselineWithItems(freeze(TargetType.ISSUE, 200L));

        BaselineDiffResponse diff = baselineDiffService.diffAgainstCurrent(10L, 300L, principal);

        assertThat(diff.items().get(0).changeType()).isEqualTo(BaselineItemChangeType.DELETED);
    }

    @Test
    void mixedItems_countsEachCategory() throws Exception {
        TestCase testCase = TestCase.builder().id(400L).project(project).tcKey("LALM-TC-1").title("로그인 성공")
                .steps("1. 입력").priority("MEDIUM").build();
        Issue issue = Issue.builder().id(200L).project(project).issueKey("LALM-1").title("버그")
                .type(IssueType.BUG).priority("LOW").status(IssueStatus.TODO).build();
        when(requirementRepository.findById(100L)).thenReturn(Optional.of(requirement));
        when(testCaseRepository.findById(400L)).thenReturn(Optional.of(testCase));
        when(issueRepository.findById(200L)).thenReturn(Optional.of(issue), Optional.empty());
        givenBaselineWithItems(freeze(TargetType.REQUIREMENT, 100L), freeze(TargetType.TEST_CASE, 400L),
                freeze(TargetType.ISSUE, 200L));

        testCase.setSteps("1. 입력\n2. 확인");

        BaselineDiffResponse diff = baselineDiffService.diffAgainstCurrent(10L, 300L, principal);

        assertThat(diff.items()).extracting(BaselineItemDiff::changeType).containsExactly(
                BaselineItemChangeType.UNCHANGED, BaselineItemChangeType.MODIFIED, BaselineItemChangeType.DELETED);
        assertThat(byField(diff.items().get(1))).containsOnlyKeys("steps");
        assertThat(diff.unchangedCount()).isEqualTo(1);
        assertThat(diff.modifiedCount()).isEqualTo(1);
        assertThat(diff.deletedCount()).isEqualTo(1);
    }

    @Test
    void baselineOfOtherProject_isNotFound() {
        when(baselineRepository.findByIdAndProjectId(300L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baselineDiffService.diffAgainstCurrent(20L, 300L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requiresViewerRole() {
        doThrow(new ForbiddenException("x")).when(projectMemberService)
                .requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.VIEWER));

        assertThatThrownBy(() -> baselineDiffService.diffAgainstCurrent(10L, 300L, principal))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(baselineRepository);
    }
}
