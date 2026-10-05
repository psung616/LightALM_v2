package com.lightalm.baseline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.baseline.domain.BaselineItem;
import com.lightalm.baseline.dto.BaselineDetailResponse;
import com.lightalm.baseline.dto.BaselineItemRef;
import com.lightalm.baseline.dto.CreateBaselineRequest;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-008(Phase 16) / 04-api.md §4.18 베이스라인 생성. {@link BaselineSnapshotFactory}·
 * {@link BaselineSnapshotJsonCodec}은 실제 인스턴스를 써서 저장되는 스냅샷 JSON 내용까지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BaselineCommandServiceTest {

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
    private PolymorphicTargetValidator polymorphicTargetValidator;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BaselineCommandService baselineCommandService;

    private Project project;
    private User admin;
    private UserPrincipal principal;
    private Requirement requirement;

    @BeforeEach
    void setUp() {
        BaselineSnapshotFactory snapshotFactory = new BaselineSnapshotFactory(requirementRepository, issueRepository, testCaseRepository);
        baselineCommandService = new BaselineCommandService(baselineRepository, baselineItemRepository, snapshotFactory,
                new BaselineSnapshotJsonCodec(objectMapper), polymorphicTargetValidator, userRepository, projectService,
                projectMemberService);

        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        admin = User.builder().id(1L).username("padmin").password("hash").email("padmin@example.com")
                .fullName("PA").systemRole(SystemRole.USER).enabled(true).build();
        User assignee = User.builder().id(7L).username("dev").password("hash").email("dev@example.com")
                .fullName("Dev").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(admin);
        requirement = Requirement.builder().id(100L).project(project).reqKey("LALM-REQ-1").title("로그인 기능")
                .type(RequirementType.FUNCTIONAL).priority("HIGH").status(RequirementStatus.DRAFT)
                .assignedTo(assignee).dueDate(LocalDate.of(2026, 12, 31)).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void create_capturesSnapshotOfCurrentValues_andDeduplicatesRefs() throws Exception {
        when(requirementRepository.findById(100L)).thenReturn(Optional.of(requirement));
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        CreateBaselineRequest request = new CreateBaselineRequest("v1.0 기준선", "릴리스 전", List.of(
                new BaselineItemRef(TargetType.REQUIREMENT, 100L),
                new BaselineItemRef(TargetType.REQUIREMENT, 100L)));

        BaselineDetailResponse response = baselineCommandService.create(10L, request, principal);

        verify(projectMemberService).requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.PROJECT_ADMIN));
        ArgumentCaptor<List<BaselineItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(baselineItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);

        JsonNode snapshot = objectMapper.readTree(captor.getValue().get(0).getSnapshot());
        assertThat(snapshot.get("key").asText()).isEqualTo("LALM-REQ-1");
        assertThat(snapshot.get("title").asText()).isEqualTo("로그인 기능");
        assertThat(snapshot.get("status").asText()).isEqualTo("DRAFT");
        assertThat(snapshot.get("priority").asText()).isEqualTo("HIGH");
        assertThat(snapshot.get("assignedToId").asLong()).isEqualTo(7L);
        assertThat(snapshot.get("dueDate").asText()).isEqualTo("2026-12-31");
        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.snapshot().get("title").asText()).isEqualTo("로그인 기능"));
        assertThat(response.name()).isEqualTo("v1.0 기준선");
        // 생성은 대상 엔티티를 읽기만 한다.
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void create_requiresProjectAdmin() {
        doThrow(new ForbiddenException("x")).when(projectMemberService)
                .requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.PROJECT_ADMIN));
        CreateBaselineRequest request = new CreateBaselineRequest("b", null,
                List.of(new BaselineItemRef(TargetType.REQUIREMENT, 100L)));

        assertThatThrownBy(() -> baselineCommandService.create(10L, request, principal)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(baselineRepository, baselineItemRepository);
    }

    @Test
    void create_withTargetOutsideProject_savesNothing() {
        doThrow(new ResourceNotFoundException("x")).when(polymorphicTargetValidator)
                .ensureExists(10L, TargetType.ISSUE, 555L);
        CreateBaselineRequest request = new CreateBaselineRequest("b", null,
                List.of(new BaselineItemRef(TargetType.ISSUE, 555L)));

        assertThatThrownBy(() -> baselineCommandService.create(10L, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(baselineRepository, never()).save(any());
        verify(baselineItemRepository, never()).saveAll(anyList());
    }
}
