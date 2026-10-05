package com.lightalm.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.review.domain.ReviewCycle;
import com.lightalm.review.domain.ReviewParticipant;
import com.lightalm.review.repository.ReviewCycleRepository;
import com.lightalm.review.repository.ReviewParticipantRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** ADR-008(Phase 16) / 04-api.md §4.17 목록 API(VIEWER+, 참여자 결정 현황 포함). */
@ExtendWith(MockitoExtension.class)
class ReviewCycleQueryServiceTest {

    @Mock
    private ReviewCycleRepository reviewCycleRepository;
    @Mock
    private ReviewParticipantRepository reviewParticipantRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private PolymorphicTargetValidator polymorphicTargetValidator;

    @InjectMocks
    private ReviewCycleQueryService reviewCycleQueryService;

    private UserPrincipal viewer;
    private User reviewer;
    private ReviewCycle cycle;

    @BeforeEach
    void setUp() {
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        reviewer = User.builder().id(2L).username("rev").password("hash").email("rev@example.com")
                .fullName("Rev").systemRole(SystemRole.USER).enabled(true).build();
        viewer = new UserPrincipal(reviewer);
        cycle = ReviewCycle.builder().project(project).targetType(TargetType.ISSUE).targetId(200L)
                .name("이슈 리뷰").createdBy(reviewer).build();
        ReflectionTestUtils.setField(cycle, "id", 51L);
    }

    @Test
    void listForTarget_includesParticipantDecisions() {
        when(reviewCycleRepository.findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(10L, TargetType.ISSUE, 200L))
                .thenReturn(List.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdOrderById(51L))
                .thenReturn(List.of(ReviewParticipant.builder().reviewCycle(cycle).user(reviewer).build()));

        assertThat(reviewCycleQueryService.listForTarget(10L, TargetType.ISSUE, 200L, viewer))
                .singleElement()
                .satisfies(c -> assertThat(c.participants()).singleElement()
                        .satisfies(p -> assertThat(p.fullName()).isEqualTo("Rev")));
    }

    @Test
    void listForTarget_requiresViewerRole() {
        doThrow(new ForbiddenException("x")).when(projectMemberService)
                .requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.VIEWER));

        assertThatThrownBy(() -> reviewCycleQueryService.listForTarget(10L, TargetType.ISSUE, 200L, viewer))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(reviewCycleRepository);
    }

    @Test
    void listForTarget_missingTarget_isNotFound() {
        doThrow(new ResourceNotFoundException("x")).when(polymorphicTargetValidator)
                .ensureExists(10L, TargetType.REQUIREMENT, 999L);

        assertThatThrownBy(() -> reviewCycleQueryService.listForTarget(10L, TargetType.REQUIREMENT, 999L, viewer))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listForTarget_rejectsTestCaseTarget() {
        assertThatThrownBy(() -> reviewCycleQueryService.listForTarget(10L, TargetType.TEST_CASE, 1L, viewer))
                .isInstanceOf(ValidationException.class);
    }
}
