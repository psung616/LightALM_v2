package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TestCaseStatus;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTestCaseRequest;
import com.lightalm.dto.TestCaseResponse;
import com.lightalm.enumeration.service.EnumerationValueValidator;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private EnumerationValueValidator enumerationValueValidator;

    @InjectMocks
    private TestCaseService testCaseService;

    private UserPrincipal principal;
    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
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
    }

    /** qa-tester 반려(2026-10-05): priority를 안 바꾸는 수정은 기존 값(폐기된 BLOCKER 포함)을 기존→신규 비교 경로로 넘긴다. */
    @Test
    void update_withoutPriorityChange_validatesAsUnchangedValue() {
        TestCase existing = TestCase.builder().id(9L).project(project).tcKey("LALM-TC9").title("old")
                .steps("s").expectedResult("e").priority("BLOCKER").build();
        when(testCaseRepository.findById(9L)).thenReturn(Optional.of(existing));
        com.lightalm.dto.UpdateTestCaseRequest request = new com.lightalm.dto.UpdateTestCaseRequest();
        request.setTitle("renamed");
        request.setSteps("s");
        request.setExpectedResult("e");

        TestCaseResponse response = testCaseService.update(10L, 9L, request, principal);

        assertThat(response.getTitle()).isEqualTo("renamed");
        assertThat(response.getPriority()).isEqualTo("BLOCKER");
        verify(enumerationValueValidator).requireValidValueForChange(10L, "PRIORITY", "BLOCKER", "BLOCKER");
    }

    @Test
    void create_generatesTcKeyAndPersistsWithDefaultDraftStatus() {
        CreateTestCaseRequest request = new CreateTestCaseRequest();
        request.setTitle("로그인 성공 검증");
        request.setSteps("1. 로그인 화면 접속\n2. 아이디/비밀번호 입력\n3. 로그인 버튼 클릭");
        request.setExpectedResult("대시보드로 이동한다");

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectService.nextTestCaseKey(10L)).thenReturn("LALM-TC1");
        when(testCaseRepository.save(org.mockito.ArgumentMatchers.any(TestCase.class))).thenAnswer(invocation -> {
            TestCase tc = invocation.getArgument(0);
            tc.setId(100L);
            tc.setCreatedAt(LocalDateTime.now());
            tc.setUpdatedAt(LocalDateTime.now());
            return tc;
        });

        TestCaseResponse response = testCaseService.create(10L, request, principal);

        assertThat(response.getTcKey()).isEqualTo("LALM-TC1");
        assertThat(response.getStatus()).isEqualTo(TestCaseStatus.DRAFT);
        assertThat(response.getPriority()).isEqualTo(Priority.MEDIUM.name());
        verify(projectMemberService).requireRole(10L, principal, ProjectRole.MEMBER);
        verify(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "MEDIUM");
    }

    @Test
    void create_whenEnumerationValidatorRejectsPriority_propagatesException() {
        CreateTestCaseRequest request = new CreateTestCaseRequest();
        request.setTitle("로그인 성공 검증");
        request.setSteps("1. 로그인 화면 접속\n2. 아이디/비밀번호 입력\n3. 로그인 버튼 클릭");
        request.setExpectedResult("대시보드로 이동한다");
        request.setPriority("BLOCKER");

        when(projectService.getEntity(10L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new com.lightalm.exception.ValidationException("유효하지 않은 PRIORITY 값입니다: BLOCKER"))
                .when(enumerationValueValidator).requireValidValue(10L, "PRIORITY", "BLOCKER");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> testCaseService.create(10L, request, principal))
                .isInstanceOf(com.lightalm.exception.ValidationException.class);
        verify(testCaseRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(TestCase.class));
    }

    @Test
    void delete_requiresProjectAdminRole() {
        TestCase existing = TestCase.builder().id(7L).project(project).tcKey("LALM-TC7").build();
        when(testCaseRepository.findById(7L)).thenReturn(Optional.of(existing));
        doNothing().when(testCaseRepository).delete(existing);

        testCaseService.delete(10L, 7L, principal);

        verify(projectMemberService).requireRole(10L, principal, ProjectRole.PROJECT_ADMIN);
        verify(testCaseRepository).delete(existing);
    }
}
