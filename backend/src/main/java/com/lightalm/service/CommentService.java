package com.lightalm.service;

import com.lightalm.domain.Comment;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.CommentResponse;
import com.lightalm.dto.CreateCommentRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.CommentRepository;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long projectId, TargetType targetType, Long targetId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        verifyTargetExists(projectId, targetType, targetId);
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long projectId, TargetType targetType, Long targetId, CreateCommentRequest request,
                                   UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        verifyTargetExists(projectId, targetType, targetId);
        Project project = projectService.getEntity(projectId);
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        Comment comment = Comment.builder()
                .project(project)
                .targetType(targetType)
                .targetId(targetId)
                .author(author)
                .content(request.getContent())
                .build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long projectId, Long commentId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));
        if (!comment.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId);
        }
        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().getId().equals(principal.getId());
        if (!isAuthor) {
            projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        }
        commentRepository.delete(comment);
    }

    private void verifyTargetExists(Long projectId, TargetType targetType, Long targetId) {
        if (targetType == TargetType.REQUIREMENT) {
            var requirement = requirementRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId));
            if (!requirement.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId);
            }
        } else {
            var issue = issueRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId));
            if (!issue.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId);
            }
        }
    }
}
