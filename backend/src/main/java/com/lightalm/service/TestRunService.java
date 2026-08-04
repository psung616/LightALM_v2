package com.lightalm.service;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Release;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TestRun;
import com.lightalm.domain.TestRunResult;
import com.lightalm.domain.TestRunStatus;
import com.lightalm.domain.User;
import com.lightalm.dto.AddTestCasesToRunRequest;
import com.lightalm.dto.ChangeTestRunStatusRequest;
import com.lightalm.dto.CreateTestRunRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RecordTestResultRequest;
import com.lightalm.dto.TestRunResponse;
import com.lightalm.dto.TestRunResultResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.ReleaseRepository;
import com.lightalm.repository.TestRunRepository;
import com.lightalm.repository.TestRunResultRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestRunResultRepository testRunResultRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final TestCaseService testCaseService;
    private final ReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public PageResponse<TestRunResponse> list(Long projectId, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return PageResponse.from(testRunRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId), pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TestRunResponse get(Long projectId, Long runId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return toResponse(getEntity(projectId, runId));
    }

    @Transactional
    public TestRunResponse create(Long projectId, CreateTestRunRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        Release release = resolveRelease(projectId, request.getReleaseId());
        TestRun run = TestRun.builder()
                .project(project)
                .release(release)
                .name(request.getName())
                .createdBy(creator)
                .build();
        return toResponse(testRunRepository.save(run));
    }

    @Transactional
    public TestRunResponse addCases(Long projectId, Long runId, AddTestCasesToRunRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        TestRun run = getEntity(projectId, runId);
        for (Long testCaseId : request.getTestCaseIds()) {
            TestCase testCase = testCaseService.getEntity(projectId, testCaseId);
            if (testRunResultRepository.findByTestRunIdAndTestCaseId(runId, testCaseId).isPresent()) {
                continue;
            }
            TestRunResult result = TestRunResult.builder()
                    .testRun(run)
                    .testCase(testCase)
                    .build();
            testRunResultRepository.save(result);
        }
        return toResponse(run);
    }

    @Transactional
    public TestRunResultResponse recordResult(Long projectId, Long runId, Long testCaseId, RecordTestResultRequest request,
                                               UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        getEntity(projectId, runId);
        TestRunResult result = testRunResultRepository.findByTestRunIdAndTestCaseId(runId, testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("테스트런에 포함되지 않은 테스트케이스입니다 — 먼저 추가하세요: " + testCaseId));
        User executor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        result.setResult(request.getResult());
        result.setActualResult(request.getActualResult());
        result.setExecutedBy(executor);
        result.setExecutedAt(LocalDateTime.now());
        return TestRunResultResponse.from(result);
    }

    @Transactional
    public TestRunResponse changeStatus(Long projectId, Long runId, ChangeTestRunStatusRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        TestRun run = getEntity(projectId, runId);
        run.setStatus(request.getStatus());
        if (request.getStatus() == TestRunStatus.IN_PROGRESS) {
            if (run.getStartedAt() == null) {
                run.setStartedAt(LocalDateTime.now());
            }
        }
        if (request.getStatus() == TestRunStatus.COMPLETED) {
            run.setCompletedAt(LocalDateTime.now());
        } else {
            run.setCompletedAt(null);
        }
        return toResponse(run);
    }

    private Release resolveRelease(Long projectId, Long releaseId) {
        if (releaseId == null) {
            return null;
        }
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("릴리스를 찾을 수 없습니다: " + releaseId));
        if (!release.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("릴리스를 찾을 수 없습니다: " + releaseId);
        }
        return release;
    }

    private TestRunResponse toResponse(TestRun run) {
        List<TestRunResult> results = testRunResultRepository.findByTestRunId(run.getId());
        return TestRunResponse.from(run, results);
    }

    TestRun getEntity(Long projectId, Long runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("테스트런을 찾을 수 없습니다: " + runId));
        if (!run.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("테스트런을 찾을 수 없습니다: " + runId);
        }
        return run;
    }
}
