package com.lightalm.web;

import com.lightalm.domain.TargetType;
import com.lightalm.dto.JenkinsBuildResponse;
import com.lightalm.dto.TriggerBuildRequest;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.JenkinsBuildService;
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

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class JenkinsBuildController {

    private final JenkinsBuildService jenkinsBuildService;

    @PostMapping("/jenkins/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void trigger(@PathVariable Long projectId, @Valid @RequestBody TriggerBuildRequest request,
                         @AuthenticationPrincipal UserPrincipal principal) {
        jenkinsBuildService.triggerBuild(projectId, request, principal);
    }

    @GetMapping("/{targetType}/{targetId}/builds")
    public List<JenkinsBuildResponse> list(@PathVariable Long projectId, @PathVariable String targetType, @PathVariable Long targetId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return jenkinsBuildService.list(projectId, toTargetType(targetType), targetId, principal);
    }

    private TargetType toTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> TargetType.REQUIREMENT;
            case "issues" -> TargetType.ISSUE;
            default -> throw new ValidationException("targetType은 requirements 또는 issues여야 합니다.");
        };
    }
}
