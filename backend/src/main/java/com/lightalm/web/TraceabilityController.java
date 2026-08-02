package com.lightalm.web;

import com.lightalm.dto.CreateTraceabilityLinkRequest;
import com.lightalm.dto.TraceabilityLinkResponse;
import com.lightalm.dto.TraceabilityMatrixResponse;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.TraceabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/traceability")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @GetMapping("/matrix")
    public TraceabilityMatrixResponse matrix(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return traceabilityService.matrix(projectId, principal);
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public TraceabilityLinkResponse createLink(@PathVariable Long projectId, @Valid @RequestBody CreateTraceabilityLinkRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return traceabilityService.createLink(projectId, request, principal);
    }

    @DeleteMapping("/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@PathVariable Long projectId, @PathVariable Long linkId, @AuthenticationPrincipal UserPrincipal principal) {
        traceabilityService.deleteLink(projectId, linkId, principal);
    }
}
