package com.lightalm.baseline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.baseline.domain.Baseline;
import com.lightalm.baseline.domain.BaselineItem;
import com.lightalm.baseline.dto.BaselineDetailResponse;
import com.lightalm.baseline.repository.BaselineItemRepository;
import com.lightalm.baseline.repository.BaselineRepository;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** ADR-008(Phase 16) / 04-api.md §4.18 베이스라인 목록·상세. */
@ExtendWith(MockitoExtension.class)
class BaselineQueryServiceTest {

    @Mock
    private BaselineRepository baselineRepository;
    @Mock
    private BaselineItemRepository baselineItemRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    private BaselineQueryService baselineQueryService;
    private UserPrincipal principal;
    private Baseline baseline;

    @BeforeEach
    void setUp() {
        baselineQueryService = new BaselineQueryService(baselineRepository, baselineItemRepository,
                new BaselineSnapshotJsonCodec(new ObjectMapper()), projectMemberService);
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        User admin = User.builder().id(1L).username("padmin").password("hash").email("padmin@example.com")
                .fullName("PA").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(admin);
        baseline = Baseline.builder().project(project).name("v1.0 기준선").createdBy(admin).build();
        ReflectionTestUtils.setField(baseline, "id", 300L);
    }

    @Test
    void list_returnsItemCounts() {
        when(baselineRepository.findByProjectIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(baseline));
        when(baselineItemRepository.countByBaselineId(300L)).thenReturn(3L);

        assertThat(baselineQueryService.list(10L, principal)).singleElement()
                .satisfies(s -> {
                    assertThat(s.itemCount()).isEqualTo(3L);
                    assertThat(s.createdByName()).isEqualTo("PA");
                });
    }

    @Test
    void get_returnsStoredSnapshotAsJsonObject() {
        BaselineItem item = BaselineItem.builder().baseline(baseline).targetType(TargetType.REQUIREMENT).targetId(100L)
                .snapshot("{\"title\":\"로그인 기능\",\"status\":\"DRAFT\"}").build();
        when(baselineRepository.findByIdAndProjectId(300L, 10L)).thenReturn(Optional.of(baseline));
        when(baselineItemRepository.findByBaselineIdOrderById(300L)).thenReturn(List.of(item));

        BaselineDetailResponse detail = baselineQueryService.get(10L, 300L, principal);

        assertThat(detail.items()).singleElement().satisfies(i -> {
            assertThat(i.snapshot().isObject()).isTrue();
            assertThat(i.snapshot().get("title").asText()).isEqualTo("로그인 기능");
        });
    }

    @Test
    void get_baselineOfOtherProject_isNotFound() {
        when(baselineRepository.findByIdAndProjectId(300L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baselineQueryService.get(20L, 300L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
