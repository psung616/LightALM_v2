package com.lightalm.web;

import com.lightalm.domain.TargetType;
import com.lightalm.dto.CreateGitLinkRequest;
import com.lightalm.dto.GitLinkResponse;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.GitLinkService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class GitLinkController {

    private final GitLinkService gitLinkService;

    @GetMapping("/{targetType}/{targetId}/git-links")
    public List<GitLinkResponse> list(@PathVariable Long projectId, @PathVariable String targetType, @PathVariable Long targetId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return gitLinkService.list(projectId, toTargetType(targetType), targetId, principal);
    }

    @PostMapping("/{targetType}/{targetId}/git-links")
    @ResponseStatus(HttpStatus.CREATED)
    public GitLinkResponse create(@PathVariable Long projectId, @PathVariable String targetType, @PathVariable Long targetId,
                                   @Valid @RequestBody CreateGitLinkRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return gitLinkService.createManual(projectId, toTargetType(targetType), targetId, request, principal);
    }

    @DeleteMapping("/git-links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long linkId, @AuthenticationPrincipal UserPrincipal principal) {
        gitLinkService.delete(projectId, linkId, principal);
    }

    private TargetType toTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> TargetType.REQUIREMENT;
            case "issues" -> TargetType.ISSUE;
            default -> throw new ValidationException("targetType은 requirements 또는 issues여야 합니다.");
        };
    }
}
