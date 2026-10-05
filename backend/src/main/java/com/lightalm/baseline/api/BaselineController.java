package com.lightalm.baseline.api;

import com.lightalm.baseline.dto.BaselineDetailResponse;
import com.lightalm.baseline.dto.BaselineDiffResponse;
import com.lightalm.baseline.dto.BaselineSummaryResponse;
import com.lightalm.baseline.dto.CreateBaselineRequest;
import com.lightalm.baseline.service.BaselineCommandService;
import com.lightalm.baseline.service.BaselineDiffService;
import com.lightalm.baseline.service.BaselineQueryService;
import com.lightalm.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADR-008(Phase 16) / 04-api.md §4.18. */
@RestController
@RequestMapping("/api/projects/{projectId}/baselines")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineCommandService baselineCommandService;
    private final BaselineQueryService baselineQueryService;
    private final BaselineDiffService baselineDiffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaselineDetailResponse create(@PathVariable Long projectId, @Valid @RequestBody CreateBaselineRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return baselineCommandService.create(projectId, request, principal);
    }

    @GetMapping
    public List<BaselineSummaryResponse> list(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return baselineQueryService.list(projectId, principal);
    }

    @GetMapping("/{baselineId}")
    public BaselineDetailResponse get(@PathVariable Long projectId, @PathVariable Long baselineId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return baselineQueryService.get(projectId, baselineId, principal);
    }

    @GetMapping("/{baselineId}/diff")
    public BaselineDiffResponse diff(@PathVariable Long projectId, @PathVariable Long baselineId,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return baselineDiffService.diffAgainstCurrent(projectId, baselineId, principal);
    }
}
