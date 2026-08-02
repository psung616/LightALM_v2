package com.lightalm.dto;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.IssueType;
import com.lightalm.domain.Priority;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueResponse {
    private Long id;
    private Long projectId;
    private String issueKey;
    private String title;
    private String description;
    private IssueType type;
    private Priority priority;
    private IssueStatus status;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public static IssueResponse from(Issue issue) {
        return IssueResponse.builder()
                .id(issue.getId())
                .projectId(issue.getProject().getId())
                .issueKey(issue.getIssueKey())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .type(issue.getType())
                .priority(issue.getPriority())
                .status(issue.getStatus())
                .reporterId(issue.getReporter() != null ? issue.getReporter().getId() : null)
                .reporterName(issue.getReporter() != null ? issue.getReporter().getFullName() : null)
                .assigneeId(issue.getAssignee() != null ? issue.getAssignee().getId() : null)
                .assigneeName(issue.getAssignee() != null ? issue.getAssignee().getFullName() : null)
                .dueDate(issue.getDueDate())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .resolvedAt(issue.getResolvedAt())
                .build();
    }
}
