package com.lightalm.web;

import com.lightalm.domain.ApprovalStatus;
import com.lightalm.dto.ApprovalDecisionRequest;
import com.lightalm.dto.ApprovalRequestResponse;
import com.lightalm.dto.PageResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalService approvalService;

    @GetMapping
    public PageResponse<ApprovalRequestResponse> list(@PathVariable Long projectId,
                                                        @RequestParam(required = false) ApprovalStatus status,
                                                        @AuthenticationPrincipal UserPrincipal principal,
                                                        Pageable pageable) {
        return approvalService.list(projectId, status, principal, pageable);
    }

    @PatchMapping("/{approvalId}/decision")
    public ApprovalRequestResponse decide(@PathVariable Long projectId, @PathVariable Long approvalId,
                                           @Valid @RequestBody ApprovalDecisionRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return approvalService.decide(projectId, approvalId, request, principal);
    }
}
