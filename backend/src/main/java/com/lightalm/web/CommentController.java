package com.lightalm.web;

import com.lightalm.domain.TargetType;
import com.lightalm.dto.CommentResponse;
import com.lightalm.dto.CreateCommentRequest;
import com.lightalm.exception.ValidationException;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{targetType}/{targetId}/comments")
    public List<CommentResponse> list(@PathVariable Long projectId, @PathVariable String targetType, @PathVariable Long targetId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.list(projectId, toTargetType(targetType), targetId, principal);
    }

    @PostMapping("/{targetType}/{targetId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable Long projectId, @PathVariable String targetType, @PathVariable Long targetId,
                                   @Valid @RequestBody CreateCommentRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.create(projectId, toTargetType(targetType), targetId, request, principal);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long commentId, @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(projectId, commentId, principal);
    }

    private TargetType toTargetType(String pathSegment) {
        return switch (pathSegment) {
            case "requirements" -> TargetType.REQUIREMENT;
            case "issues" -> TargetType.ISSUE;
            default -> throw new ValidationException("targetType은 requirements 또는 issues여야 합니다.");
        };
    }
}
