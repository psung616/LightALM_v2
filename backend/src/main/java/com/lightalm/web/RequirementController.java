package com.lightalm.web;

import com.lightalm.domain.Priority;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import com.lightalm.dto.ChangeRequirementStatusRequest;
import com.lightalm.dto.CreateRequirementRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RequirementLinkResponse;
import com.lightalm.dto.RequirementResponse;
import com.lightalm.dto.TraceabilityTreeResponse;
import com.lightalm.dto.UpdateRequirementRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.RequirementService;
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
@RequestMapping("/api/projects/{projectId}/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;
    private final TraceabilityService traceabilityService;

    @GetMapping
    public PageResponse<RequirementResponse> list(@PathVariable Long projectId,
                                                    @RequestParam(required = false) RequirementStatus status,
                                                    @RequestParam(required = false) RequirementType type,
                                                    @RequestParam(required = false) Priority priority,
                                                    @RequestParam(required = false) Long parentId,
                                                    @RequestParam(required = false) Long assignedTo,
                                                    @RequestParam(required = false) String keyword,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    Pageable pageable) {
        return requirementService.list(projectId, status, type, priority, parentId, assignedTo, keyword, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateRequirementRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return requirementService.create(projectId, request, principal);
    }

    @GetMapping("/{reqId}")
    public RequirementResponse get(@PathVariable Long projectId, @PathVariable Long reqId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return requirementService.get(projectId, reqId, principal);
    }

    @PutMapping("/{reqId}")
    public RequirementResponse update(@PathVariable Long projectId, @PathVariable Long reqId,
                                       @Valid @RequestBody UpdateRequirementRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return requirementService.update(projectId, reqId, request, principal);
    }

    @PatchMapping("/{reqId}/status")
    public RequirementResponse changeStatus(@PathVariable Long projectId, @PathVariable Long reqId,
                                             @Valid @RequestBody ChangeRequirementStatusRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return requirementService.changeStatus(projectId, reqId, request, principal);
    }

    @DeleteMapping("/{reqId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long reqId, @AuthenticationPrincipal UserPrincipal principal) {
        requirementService.delete(projectId, reqId, principal);
    }

    @GetMapping("/{reqId}/children")
    public List<RequirementResponse> children(@PathVariable Long projectId, @PathVariable Long reqId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return requirementService.children(projectId, reqId, principal);
    }

    @GetMapping("/{reqId}/links")
    public List<RequirementLinkResponse> links(@PathVariable Long projectId, @PathVariable Long reqId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return traceabilityService.requirementLinks(projectId, reqId, principal);
    }

    @GetMapping("/{reqId}/traceability-tree")
    public TraceabilityTreeResponse traceabilityTree(@PathVariable Long projectId, @PathVariable Long reqId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return traceabilityService.tree(projectId, reqId, principal);
    }
}
