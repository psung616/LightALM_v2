package com.lightalm.web;

import com.lightalm.domain.Priority;
import com.lightalm.domain.TestCaseStatus;
import com.lightalm.dto.CreateTestCaseRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.TestCaseResponse;
import com.lightalm.dto.UpdateTestCaseRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.TestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/test-cases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping
    public PageResponse<TestCaseResponse> list(@PathVariable Long projectId,
                                                @RequestParam(required = false) Long requirementId,
                                                @RequestParam(required = false) TestCaseStatus status,
                                                @RequestParam(required = false) Priority priority,
                                                @RequestParam(required = false) String keyword,
                                                @AuthenticationPrincipal UserPrincipal principal,
                                                Pageable pageable) {
        return testCaseService.list(projectId, requirementId, status, priority, keyword, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateTestCaseRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return testCaseService.create(projectId, request, principal);
    }

    @GetMapping("/{tcId}")
    public TestCaseResponse get(@PathVariable Long projectId, @PathVariable Long tcId,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return testCaseService.get(projectId, tcId, principal);
    }

    @PutMapping("/{tcId}")
    public TestCaseResponse update(@PathVariable Long projectId, @PathVariable Long tcId,
                                    @Valid @RequestBody UpdateTestCaseRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return testCaseService.update(projectId, tcId, request, principal);
    }

    @DeleteMapping("/{tcId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long tcId, @AuthenticationPrincipal UserPrincipal principal) {
        testCaseService.delete(projectId, tcId, principal);
    }
}
