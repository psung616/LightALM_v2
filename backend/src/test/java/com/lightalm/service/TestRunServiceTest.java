package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Project;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TestRun;
import com.lightalm.domain.TestRunStatus;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangeTestRunStatusRequest;
import com.lightalm.dto.TestRunResponse;
import com.lightalm.repository.TestRunRepository;
import com.lightalm.repository.TestRunResultRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestRunServiceTest {

    @Mock
    private TestRunRepository testRunRepository;
    @Mock
    private TestRunResultRepository testRunResultRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private TestCaseService testCaseService;

    @InjectMocks
    private TestRunService testRunService;

    private UserPrincipal principal;
    private Project project;

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
        project = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        when(testRunResultRepository.findByTestRunId(5L)).thenReturn(Collections.emptyList());
    }

    @Test
    void changeStatus_toInProgress_setsStartedAtWhenAbsent() {
        TestRun run = TestRun.builder().id(5L).project(project).name("Sprint 1 회귀 테스트").status(TestRunStatus.PLANNED).build();
        when(testRunRepository.findById(5L)).thenReturn(Optional.of(run));

        ChangeTestRunStatusRequest request = new ChangeTestRunStatusRequest();
        request.setStatus(TestRunStatus.IN_PROGRESS);

        TestRunResponse response = testRunService.changeStatus(10L, 5L, request, principal);

        assertThat(response.getStatus()).isEqualTo(TestRunStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getCompletedAt()).isNull();
    }

    @Test
    void changeStatus_completedThenMovedAway_clearsCompletedAt() {
        TestRun run = TestRun.builder().id(5L).project(project).name("Sprint 1 회귀 테스트").status(TestRunStatus.IN_PROGRESS).build();
        when(testRunRepository.findById(5L)).thenReturn(Optional.of(run));

        ChangeTestRunStatusRequest completeRequest = new ChangeTestRunStatusRequest();
        completeRequest.setStatus(TestRunStatus.COMPLETED);
        TestRunResponse completed = testRunService.changeStatus(10L, 5L, completeRequest, principal);
        assertThat(completed.getCompletedAt()).isNotNull();

        ChangeTestRunStatusRequest reopenRequest = new ChangeTestRunStatusRequest();
        reopenRequest.setStatus(TestRunStatus.IN_PROGRESS);
        TestRunResponse reopened = testRunService.changeStatus(10L, 5L, reopenRequest, principal);
        assertThat(reopened.getCompletedAt()).isNull();
    }
}
