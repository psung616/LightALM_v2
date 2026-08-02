package com.lightalm.web;

import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.Priority;
import com.lightalm.dto.ChangeIssueStatusRequest;
import com.lightalm.dto.CreateIssueRequest;
import com.lightalm.dto.IssueResponse;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RequirementLinkResponse;
import com.lightalm.dto.UpdateIssueRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.IssueService;
import com.lightalm.service.TraceabilityService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final TraceabilityService traceabilityService;

    @GetMapping
    public PageResponse<IssueResponse> list(@PathVariable Long projectId,
                                             @RequestParam(required = false) IssueStatus status,
                                             @RequestParam(required = false) IssueType type,
                                             @RequestParam(required = false) Priority priority,
                                             @RequestParam(required = false) Long assigneeId,
                                             @RequestParam(required = false) String keyword,
                                             @AuthenticationPrincipal UserPrincipal principal,
                                             Pageable pageable) {
        return issueService.list(projectId, status, type, priority, assigneeId, keyword, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateIssueRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.create(projectId, request, principal);
    }

    @GetMapping("/{issueId}")
    public IssueResponse get(@PathVariable Long projectId, @PathVariable Long issueId,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.get(projectId, issueId, principal);
    }

    @PutMapping("/{issueId}")
    public IssueResponse update(@PathVariable Long projectId, @PathVariable Long issueId,
                                 @Valid @RequestBody UpdateIssueRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.update(projectId, issueId, request, principal);
    }

    @PatchMapping("/{issueId}/status")
    public IssueResponse changeStatus(@PathVariable Long projectId, @PathVariable Long issueId,
                                       @Valid @RequestBody ChangeIssueStatusRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.changeStatus(projectId, issueId, request, principal);
    }

    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long issueId, @AuthenticationPrincipal UserPrincipal principal) {
        issueService.delete(projectId, issueId, principal);
    }

    @GetMapping("/{issueId}/links")
    public List<RequirementLinkResponse> links(@PathVariable Long projectId, @PathVariable Long issueId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return traceabilityService.issueLinks(projectId, issueId, principal);
    }
}
