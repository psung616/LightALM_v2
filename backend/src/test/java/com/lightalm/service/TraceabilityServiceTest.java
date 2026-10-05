package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.LinkType;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TestCaseStatus;
import com.lightalm.domain.TraceabilityLink;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTraceabilityLinkRequest;
import com.lightalm.dto.RequirementLinkResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.repository.TraceabilityLinkRepository;
import com.lightalm.repository.TraceabilityTreeRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.support.PolymorphicTargetValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-010: TraceabilityService.createLink()가 다른 프로젝트 소속 대상을 다룰 때
 * 기존 ValidationException(400) 대신 ResourceNotFoundException(404)을 반환하도록
 * PolymorphicTargetValidator에 위임하는 변경의 회귀 테스트.
 */
@ExtendWith(MockitoExtension.class)
class TraceabilityServiceTest {

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;
    @Mock
    private TraceabilityTreeRepository traceabilityTreeRepository;
    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private PolymorphicTargetValidator polymorphicTargetValidator;

    @InjectMocks
    private TraceabilityService traceabilityService;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("member1")
                .password("hash")
                .email("member1@example.com")
                .fullName("Member One")
                .systemRole(SystemRole.USER)
                .enabled(true)
                .build();
        principal = new UserPrincipal(user);
    }

    @Test
    void createLink_whenSourceBelongsToOtherProject_propagatesResourceNotFound() {
        CreateTraceabilityLinkRequest request = new CreateTraceabilityLinkRequest();
        request.setSourceType(TargetType.REQUIREMENT);
        request.setSourceId(1L);
        request.setTargetType(TargetType.ISSUE);
        request.setTargetId(2L);
        request.setLinkType(LinkType.IMPLEMENTS);

        doThrow(new ResourceNotFoundException("요구사항을 찾을 수 없습니다: 1"))
                .when(polymorphicTargetValidator).ensureExists(10L, TargetType.REQUIREMENT, 1L);

        assertThatThrownBy(() -> traceabilityService.createLink(10L, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * ADR-013 버그 회귀 테스트(qa-tester 발견): source_type=TEST_CASE인 링크를
     * GET .../issues/{issueId}/links로 조회하면, 기존 코드는 otherType이 ISSUE가
     * 아니면 무조건 REQUIREMENT로 간주해 테스트케이스 대신 엉뚱한 요구사항을 반환했다.
     * 이제는 TEST_CASE를 정확히 식별해 테스트케이스 정보를 반환해야 한다.
     */
    @Test
    void issueLinks_whenIncomingLinkSourceIsTestCase_returnsTestCaseDetailsNotRequirement() {
        Project project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        Issue issue = Issue.builder()
                .id(7L).project(project).issueKey("LALM-7").title("발견된 결함")
                .type(IssueType.BUG).status(IssueStatus.TODO).build();
        TestCase testCase = TestCase.builder()
                .id(5L).project(project).tcKey("LALM-TC5").title("로그인 실패 테스트")
                .steps("1. 로그인 시도").expectedResult("실패 메시지 표시")
                .status(TestCaseStatus.READY).build();
        TraceabilityLink link = TraceabilityLink.builder()
                .id(1L).project(project)
                .sourceType(TargetType.TEST_CASE).sourceId(5L)
                .targetType(TargetType.ISSUE).targetId(7L)
                .linkType(LinkType.RELATES_TO)
                .build();

        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(traceabilityLinkRepository.findBySourceTypeAndSourceId(TargetType.ISSUE, 7L)).thenReturn(List.of());
        when(traceabilityLinkRepository.findByTargetTypeAndTargetId(TargetType.ISSUE, 7L)).thenReturn(List.of(link));
        when(testCaseRepository.findById(5L)).thenReturn(Optional.of(testCase));

        List<RequirementLinkResponse> result = traceabilityService.issueLinks(10L, 7L, principal);

        assertThat(result).hasSize(1);
        RequirementLinkResponse response = result.get(0);
        assertThat(response.getLinkedType()).isEqualTo(TargetType.TEST_CASE);
        assertThat(response.getLinkedId()).isEqualTo(5L);
        assertThat(response.getLinkedKey()).isEqualTo("LALM-TC5");
        assertThat(response.getLinkedTitle()).isEqualTo("로그인 실패 테스트");
    }
}
