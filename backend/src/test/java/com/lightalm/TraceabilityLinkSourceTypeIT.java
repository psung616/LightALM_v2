package com.lightalm;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.LinkType;
import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TraceabilityLink;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTraceabilityLinkRequest;
import com.lightalm.dto.RequirementLinkResponse;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.repository.TraceabilityLinkRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.TraceabilityService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ADR-013: traceability_links.source_type CHECK 제약이 'TEST_CASE'까지 허용하도록
 * 넓어졌는지(V12__widen_traceability_links_source_type.sql) 실제 DB에 대해 검증하는 회귀 테스트.
 * Testcontainers가 필요하므로 *IT.java로 명명되어 mvn verify(failsafe)에서만 실행된다.
 */
@Testcontainers
@SpringBootTest
class TraceabilityLinkSourceTypeIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private TestCaseRepository testCaseRepository;
    @Autowired
    private TraceabilityLinkRepository traceabilityLinkRepository;
    @Autowired
    private TraceabilityService traceabilityService;

    @Test
    void createLink_withTestCaseAsSource_isPersistedSuccessfully() {
        User creator = userRepository.save(User.builder()
                .username("tc-source-test")
                .password("hash")
                .email("tc-source-test@example.com")
                .fullName("TC Source Test")
                .build());
        Project project = projectRepository.save(Project.builder()
                .projectKey("TCSRC")
                .name("TestCase Source Link Project")
                .createdBy(creator)
                .build());
        Issue issue = issueRepository.save(Issue.builder()
                .project(project)
                .issueKey("TCSRC-1")
                .title("발견된 결함")
                .type(IssueType.BUG)
                .status(IssueStatus.TODO)
                .build());
        TestCase testCase = testCaseRepository.save(TestCase.builder()
                .project(project)
                .tcKey("TCSRC-TC1")
                .title("로그인 실패 테스트케이스")
                .steps("1. 로그인 시도")
                .expectedResult("실패 메시지 표시")
                .priority(Priority.MEDIUM.name())
                .build());

        TraceabilityLink link = traceabilityLinkRepository.save(TraceabilityLink.builder()
                .project(project)
                .sourceType(TargetType.TEST_CASE)
                .sourceId(testCase.getId())
                .targetType(TargetType.ISSUE)
                .targetId(issue.getId())
                .linkType(LinkType.RELATES_TO)
                .createdBy(creator)
                .build());

        assertThat(link.getId()).isNotNull();
        assertThat(traceabilityLinkRepository.findBySourceTypeAndSourceId(TargetType.TEST_CASE, testCase.getId()))
                .hasSize(1);
    }

    /**
     * ADR-013 버그 회귀 테스트(qa-tester 발견): qa-tester가 제보한 재현 절차를 그대로 따른다
     * (POST .../traceability/links로 TEST_CASE source 링크 생성 후 GET .../issues/{issueId}/links 조회).
     * 수정 전에는 linkedType이 REQUIREMENT로, linkedId가 우연히 같은 엉뚱한 요구사항으로 표시됐다.
     */
    @Test
    void issueLinks_afterCreatingLinkWithTestCaseAsSource_returnsTestCaseNotRequirement() {
        User creator = userRepository.save(User.builder()
                .username("tc-source-read-test")
                .password("hash")
                .email("tc-source-read-test@example.com")
                .fullName("TC Source Read Test")
                .systemRole(SystemRole.ADMIN)
                .build());
        Project project = projectRepository.save(Project.builder()
                .projectKey("TCREAD")
                .name("TestCase Source Link Read Project")
                .createdBy(creator)
                .build());
        Issue issue = issueRepository.save(Issue.builder()
                .project(project)
                .issueKey("TCREAD-1")
                .title("발견된 결함")
                .type(IssueType.BUG)
                .status(IssueStatus.TODO)
                .build());
        TestCase testCase = testCaseRepository.save(TestCase.builder()
                .project(project)
                .tcKey("TCREAD-TC1")
                .title("로그인 실패 테스트케이스")
                .steps("1. 로그인 시도")
                .expectedResult("실패 메시지 표시")
                .priority(Priority.MEDIUM.name())
                .build());

        UserPrincipal principal = new UserPrincipal(creator);
        CreateTraceabilityLinkRequest request = new CreateTraceabilityLinkRequest();
        request.setSourceType(TargetType.TEST_CASE);
        request.setSourceId(testCase.getId());
        request.setTargetType(TargetType.ISSUE);
        request.setTargetId(issue.getId());
        request.setLinkType(LinkType.RELATES_TO);
        traceabilityService.createLink(project.getId(), request, principal);

        List<RequirementLinkResponse> links = traceabilityService.issueLinks(project.getId(), issue.getId(), principal);

        assertThat(links).hasSize(1);
        RequirementLinkResponse response = links.get(0);
        assertThat(response.getLinkedType()).isEqualTo(TargetType.TEST_CASE);
        assertThat(response.getLinkedId()).isEqualTo(testCase.getId());
        assertThat(response.getLinkedKey()).isEqualTo("TCREAD-TC1");
        assertThat(response.getLinkedTitle()).isEqualTo("로그인 실패 테스트케이스");
    }
}
