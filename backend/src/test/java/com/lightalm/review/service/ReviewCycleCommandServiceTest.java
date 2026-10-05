package com.lightalm.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import com.lightalm.review.domain.ReviewCycle;
import com.lightalm.review.domain.ReviewCycleStatus;
import com.lightalm.review.domain.ReviewDecision;
import com.lightalm.review.domain.ReviewParticipant;
import com.lightalm.review.dto.CreateReviewCycleRequest;
import com.lightalm.review.dto.RecordReviewDecisionRequest;
import com.lightalm.review.dto.ReviewCycleDetailResponse;
import com.lightalm.review.dto.ReviewParticipantResponse;
import com.lightalm.review.repository.ReviewCycleRepository;
import com.lightalm.review.repository.ReviewParticipantRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import com.lightalm.service.ProjectService;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ADR-008(Phase 16) / 08-dev-phases.md Phase 16 DoD. 특히 "참여자 결정 기록·사이클 닫기가 대상
 * (요구사항/이슈)의 status를 절대 바꾸지 않는다"(03-data-model.md §3.19)와 "참여자가 아닌 사용자의
 * 결정 기록 거부"(04-api.md §4.17)를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ReviewCycleCommandServiceTest {

    @Mock
    private ReviewCycleRepository reviewCycleRepository;
    @Mock
    private ReviewParticipantRepository reviewParticipantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private PolymorphicTargetValidator polymorphicTargetValidator;

    @InjectMocks
    private ReviewCycleCommandService reviewCycleCommandService;

    private Project project;
    private User creator;
    private User reviewerA;
    private User reviewerB;
    private User outsider;
    private Requirement requirement;
    private ReviewCycle cycle;
    private ReviewParticipant participantA;
    private ReviewParticipant participantB;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        creator = user(1L, "creator", SystemRole.USER);
        reviewerA = user(2L, "reviewerA", SystemRole.USER);
        reviewerB = user(3L, "reviewerB", SystemRole.USER);
        outsider = user(4L, "outsider", SystemRole.USER);
        requirement = Requirement.builder().id(100L).project(project).reqKey("LALM-REQ-1").title("로그인")
                .status(RequirementStatus.DRAFT).build();

        cycle = ReviewCycle.builder().project(project).targetType(TargetType.REQUIREMENT).targetId(100L)
                .name("1차 리뷰").createdBy(creator).build();
        ReflectionTestUtils.setField(cycle, "id", 50L);
        participantA = ReviewParticipant.builder().reviewCycle(cycle).user(reviewerA).build();
        participantB = ReviewParticipant.builder().reviewCycle(cycle).user(reviewerB).build();
    }

    private static User user(Long id, String username, SystemRole role) {
        return User.builder().id(id).username(username).password("hash").email(username + "@example.com")
                .fullName(username).systemRole(role).enabled(true).build();
    }

    @Test
    void create_savesCycleWithPendingParticipants() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(any())).thenReturn(List.of(reviewerA, reviewerB));
        when(projectMemberService.findRole(10L, 2L)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(projectMemberService.findRole(10L, 3L)).thenReturn(Optional.of(ProjectRole.VIEWER));

        ReviewCycleDetailResponse response = reviewCycleCommandService.create(10L, TargetType.REQUIREMENT, 100L,
                new CreateReviewCycleRequest("1차 리뷰", List.of(2L, 3L, 2L)), new UserPrincipal(creator));

        assertThat(response.status()).isEqualTo(ReviewCycleStatus.OPEN);
        assertThat(response.participants()).hasSize(2)
                .allSatisfy(p -> assertThat(p.decision()).isEqualTo(ReviewDecision.PENDING));
        verify(projectMemberService).requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.MEMBER));
        verify(polymorphicTargetValidator).ensureExists(10L, TargetType.REQUIREMENT, 100L);
    }

    @Test
    void create_rejectsTestCaseTarget() {
        assertThatThrownBy(() -> reviewCycleCommandService.create(10L, TargetType.TEST_CASE, 1L,
                new CreateReviewCycleRequest("x", List.of(2L)), new UserPrincipal(creator)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsNonMemberParticipant() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(any())).thenReturn(List.of(outsider));
        when(projectMemberService.findRole(10L, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewCycleCommandService.create(10L, TargetType.REQUIREMENT, 100L,
                new CreateReviewCycleRequest("1차 리뷰", List.of(4L)), new UserPrincipal(creator)))
                .isInstanceOf(ValidationException.class);
        verify(reviewCycleRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownParticipantId() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(any())).thenReturn(List.of(reviewerA));

        assertThatThrownBy(() -> reviewCycleCommandService.create(10L, TargetType.REQUIREMENT, 100L,
                new CreateReviewCycleRequest("1차 리뷰", List.of(2L, 999L)), new UserPrincipal(creator)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_requiresMemberRole() {
        doThrow(new ForbiddenException("x")).when(projectMemberService)
                .requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.MEMBER));

        assertThatThrownBy(() -> reviewCycleCommandService.create(10L, TargetType.REQUIREMENT, 100L,
                new CreateReviewCycleRequest("x", List.of(2L)), new UserPrincipal(outsider)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(reviewCycleRepository);
    }

    @Test
    void recordMyDecision_byMultipleParticipants_neverChangesTargetStatus() {
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdAndUserId(50L, 2L)).thenReturn(Optional.of(participantA));
        when(reviewParticipantRepository.findByReviewCycleIdAndUserId(50L, 3L)).thenReturn(Optional.of(participantB));
        when(reviewParticipantRepository.findByReviewCycleIdOrderById(50L)).thenReturn(List.of(participantA, participantB));

        reviewCycleCommandService.recordMyDecision(10L, 50L,
                new RecordReviewDecisionRequest(ReviewDecision.APPROVE, "좋음"), new UserPrincipal(reviewerA));
        ReviewCycleDetailResponse response = reviewCycleCommandService.recordMyDecision(10L, 50L,
                new RecordReviewDecisionRequest(ReviewDecision.APPROVE, "동의"), new UserPrincipal(reviewerB));

        assertThat(participantA.getDecision()).isEqualTo(ReviewDecision.APPROVE);
        assertThat(participantA.getDecidedAt()).isNotNull();
        assertThat(participantB.getComment()).isEqualTo("동의");
        assertThat(response.participants()).extracting(ReviewParticipantResponse::decision)
                .containsExactly(ReviewDecision.APPROVE, ReviewDecision.APPROVE);
        // 전원 승인이어도 사이클 자체와 대상 요구사항의 status는 그대로다(§3.19).
        assertThat(cycle.getStatus()).isEqualTo(ReviewCycleStatus.OPEN);
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.DRAFT);
        // 대상 검증 컴포넌트를 포함해 대상 엔티티를 다룰 수 있는 협력 객체를 아예 호출하지 않는다.
        verifyNoInteractions(polymorphicTargetValidator, projectService, userRepository);
    }

    @Test
    void close_doesNotChangeTargetStatus() {
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdOrderById(50L)).thenReturn(List.of(participantA, participantB));
        participantA.recordDecision(ReviewDecision.REJECT, "반대");

        ReviewCycleDetailResponse response = reviewCycleCommandService.close(10L, 50L, new UserPrincipal(creator));

        assertThat(response.status()).isEqualTo(ReviewCycleStatus.CLOSED);
        assertThat(response.closedAt()).isNotNull();
        assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.DRAFT);
        verify(projectMemberService).requireRole(eq(10L), any(UserPrincipal.class), eq(ProjectRole.MEMBER));
        verifyNoInteractions(polymorphicTargetValidator, projectService, userRepository);
        verify(reviewParticipantRepository, never()).saveAll(anyIterable());
    }

    /**
     * 구조적 보장: 리뷰 사이클 서비스가 요구사항/이슈의 status를 바꿀 수 있는 협력 객체(Repository/Service)를
     * 의존성으로 아예 갖지 않는다. 누군가 "전원 승인 시 자동 APPROVED" 같은 로직을 추가하려고
     * RequirementRepository 등을 주입하면 이 테스트가 깨져 §3.19 원칙 위반을 알린다.
     */
    @Test
    void reviewServices_haveNoDependencyCapableOfChangingTargetStatus() {
        List<String> dependencyTypes = Stream.of(ReviewCycleCommandService.class, ReviewCycleQueryService.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .map(Field::getType)
                .map(Class::getSimpleName)
                .toList();

        assertThat(dependencyTypes).isNotEmpty().noneMatch(name -> name.startsWith("Requirement")
                || name.startsWith("Issue")
                || name.startsWith("TestCase")
                || name.startsWith("Approval")
                || name.startsWith("Workflow"));
    }

    @Test
    void recordMyDecision_byNonParticipant_isForbiddenEvenForProjectAdmin() {
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewCycleCommandService.recordMyDecision(10L, 50L,
                new RecordReviewDecisionRequest(ReviewDecision.APPROVE, null), new UserPrincipal(creator)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(participantA.getDecision()).isEqualTo(ReviewDecision.PENDING);
    }

    @Test
    void recordMyDecision_onClosedCycle_isRejected() {
        cycle.close();
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdAndUserId(50L, 2L)).thenReturn(Optional.of(participantA));

        assertThatThrownBy(() -> reviewCycleCommandService.recordMyDecision(10L, 50L,
                new RecordReviewDecisionRequest(ReviewDecision.APPROVE, null), new UserPrincipal(reviewerA)))
                .isInstanceOf(ValidationException.class);
        assertThat(participantA.getDecision()).isEqualTo(ReviewDecision.PENDING);
    }

    @Test
    void recordMyDecision_withPendingValue_isRejected() {
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));
        when(reviewParticipantRepository.findByReviewCycleIdAndUserId(50L, 2L)).thenReturn(Optional.of(participantA));

        assertThatThrownBy(() -> reviewCycleCommandService.recordMyDecision(10L, 50L,
                new RecordReviewDecisionRequest(ReviewDecision.PENDING, null), new UserPrincipal(reviewerA)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void close_alreadyClosedCycle_isRejected() {
        cycle.close();
        when(reviewCycleRepository.findByIdAndProjectId(50L, 10L)).thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> reviewCycleCommandService.close(10L, 50L, new UserPrincipal(creator)))
                .isInstanceOf(ValidationException.class);
    }
}
