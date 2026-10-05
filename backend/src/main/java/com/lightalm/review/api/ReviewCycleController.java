package com.lightalm.review.api;

import com.lightalm.domain.TargetType;
import com.lightalm.exception.ValidationException;
import com.lightalm.review.dto.CreateReviewCycleRequest;
import com.lightalm.review.dto.RecordReviewDecisionRequest;
import com.lightalm.review.dto.ReviewCycleDetailResponse;
import com.lightalm.review.service.ReviewCycleCommandService;
import com.lightalm.review.service.ReviewCycleQueryService;
import com.lightalm.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/** ADR-008(Phase 16) / 04-api.md §4.17. */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ReviewCycleController {

    private final ReviewCycleCommandService reviewCycleCommandService;
    private final ReviewCycleQueryService reviewCycleQueryService;

    @PostMapping("/{targetType}/{targetId}/review-cycles")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCycleDetailResponse create(@PathVariable Long projectId, @PathVariable String targetType,
                                            @PathVariable Long targetId, @Valid @RequestBody CreateReviewCycleRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return reviewCycleCommandService.create(projectId, toTargetType(targetType), targetId, request, principal);
    }

    @GetMapping("/{targetType}/{targetId}/review-cycles")
    public List<ReviewCycleDetailResponse> list(@PathVariable Long projectId, @PathVariable String targetType,
                                                @PathVariable Long targetId, @AuthenticationPrincipal UserPrincipal principal) {
        return reviewCycleQueryService.listForTarget(projectId, toTargetType(targetType), targetId, principal);
    }

    @PatchMapping("/review-cycles/{cycleId}/participants/me")
    public ReviewCycleDetailResponse recordMyDecision(@PathVariable Long projectId, @PathVariable Long cycleId,
                                                      @Valid @RequestBody RecordReviewDecisionRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return reviewCycleCommandService.recordMyDecision(projectId, cycleId, request, principal);
    }

    /** 사이클 닫기(항상 CLOSED). 요청 body는 받지 않는다 — 재오픈 API 없음. */
    @PatchMapping("/review-cycles/{cycleId}/status")
    public ReviewCycleDetailResponse close(@PathVariable Long projectId, @PathVariable Long cycleId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return reviewCycleCommandService.close(projectId, cycleId, principal);
    }

    private TargetType toTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> TargetType.REQUIREMENT;
            case "issues" -> TargetType.ISSUE;
            default -> throw new ValidationException("targetType은 requirements 또는 issues여야 합니다.");
        };
    }
}
