package com.lightalm.web;

import com.lightalm.dto.AddTestCasesToRunRequest;
import com.lightalm.dto.ChangeTestRunStatusRequest;
import com.lightalm.dto.CreateTestRunRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RecordTestResultRequest;
import com.lightalm.dto.TestRunResponse;
import com.lightalm.dto.TestRunResultResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.TestRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/test-runs")
@RequiredArgsConstructor
public class TestRunController {

    private final TestRunService testRunService;

    @GetMapping
    public PageResponse<TestRunResponse> list(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
                                               Pageable pageable) {
        return testRunService.list(projectId, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestRunResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateTestRunRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return testRunService.create(projectId, request, principal);
    }

    @GetMapping("/{runId}")
    public TestRunResponse get(@PathVariable Long projectId, @PathVariable Long runId,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return testRunService.get(projectId, runId, principal);
    }

    @PostMapping("/{runId}/cases")
    public TestRunResponse addCases(@PathVariable Long projectId, @PathVariable Long runId,
                                     @Valid @RequestBody AddTestCasesToRunRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return testRunService.addCases(projectId, runId, request, principal);
    }

    @PatchMapping("/{runId}/results/{testCaseId}")
    public TestRunResultResponse recordResult(@PathVariable Long projectId, @PathVariable Long runId, @PathVariable Long testCaseId,
                                               @Valid @RequestBody RecordTestResultRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return testRunService.recordResult(projectId, runId, testCaseId, request, principal);
    }

    @PatchMapping("/{runId}/status")
    public TestRunResponse changeStatus(@PathVariable Long projectId, @PathVariable Long runId,
                                         @Valid @RequestBody ChangeTestRunStatusRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return testRunService.changeStatus(projectId, runId, request, principal);
    }
}
