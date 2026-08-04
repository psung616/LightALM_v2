package com.lightalm.web;

import com.lightalm.dto.AddReleaseItemRequest;
import com.lightalm.dto.ChangeReleaseStatusRequest;
import com.lightalm.dto.CreateReleaseRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.ReleaseNotesResponse;
import com.lightalm.dto.ReleaseResponse;
import com.lightalm.dto.UpdateReleaseRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ReleaseService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public PageResponse<ReleaseResponse> list(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
                                               Pageable pageable) {
        return releaseService.list(projectId, principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateReleaseRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.create(projectId, request, principal);
    }

    @GetMapping("/{releaseId}")
    public ReleaseResponse get(@PathVariable Long projectId, @PathVariable Long releaseId,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.get(projectId, releaseId, principal);
    }

    @PutMapping("/{releaseId}")
    public ReleaseResponse update(@PathVariable Long projectId, @PathVariable Long releaseId,
                                   @Valid @RequestBody UpdateReleaseRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.update(projectId, releaseId, request, principal);
    }

    @PatchMapping("/{releaseId}/status")
    public ReleaseResponse changeStatus(@PathVariable Long projectId, @PathVariable Long releaseId,
                                         @Valid @RequestBody ChangeReleaseStatusRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.changeStatus(projectId, releaseId, request, principal);
    }

    @PostMapping("/{releaseId}/items")
    public ReleaseResponse addItem(@PathVariable Long projectId, @PathVariable Long releaseId,
                                    @Valid @RequestBody AddReleaseItemRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.addItem(projectId, releaseId, request, principal);
    }

    @DeleteMapping("/{releaseId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long projectId, @PathVariable Long releaseId, @PathVariable Long itemId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        releaseService.removeItem(projectId, releaseId, itemId, principal);
    }

    @GetMapping("/{releaseId}/notes")
    public ReleaseNotesResponse notes(@PathVariable Long projectId, @PathVariable Long releaseId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return releaseService.generateNotes(projectId, releaseId, principal);
    }
}
