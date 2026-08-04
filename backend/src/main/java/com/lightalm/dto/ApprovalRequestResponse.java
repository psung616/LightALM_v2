package com.lightalm.dto;

import com.lightalm.domain.ApprovalRequest;
import com.lightalm.domain.ApprovalStatus;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.TargetType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalRequestResponse {
    private Long id;
    private Long projectId;
    private TargetType targetType;
    private Long targetId;
    private String targetKey;
    private String targetTitle;
    private RequirementStatus requestedStatus;
    private Long requestedById;
    private String requestedByName;
    private ApprovalStatus status;
    private Long approverId;
    private String approverName;
    private String comment;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;

    public static ApprovalRequestResponse from(ApprovalRequest approval, String targetKey, String targetTitle) {
        return ApprovalRequestResponse.builder()
                .id(approval.getId())
                .projectId(approval.getProject().getId())
                .targetType(approval.getTargetType())
                .targetId(approval.getTargetId())
                .targetKey(targetKey)
                .targetTitle(targetTitle)
                .requestedStatus(approval.getRequestedStatus())
                .requestedById(approval.getRequestedBy() != null ? approval.getRequestedBy().getId() : null)
                .requestedByName(approval.getRequestedBy() != null ? approval.getRequestedBy().getFullName() : null)
                .status(approval.getStatus())
                .approverId(approval.getApprover() != null ? approval.getApprover().getId() : null)
                .approverName(approval.getApprover() != null ? approval.getApprover().getFullName() : null)
                .comment(approval.getComment())
                .requestedAt(approval.getRequestedAt())
                .resolvedAt(approval.getResolvedAt())
                .build();
    }
}
