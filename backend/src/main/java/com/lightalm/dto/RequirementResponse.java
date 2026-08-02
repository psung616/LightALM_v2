package com.lightalm.dto;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.domain.RequirementType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequirementResponse {
    private Long id;
    private Long projectId;
    private String reqKey;
    private String title;
    private String description;
    private RequirementType type;
    private Priority priority;
    private RequirementStatus status;
    private Long parentRequirementId;
    private String parentRequirementKey;
    private Long createdById;
    private String createdByName;
    private Long assignedToId;
    private String assignedToName;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RequirementResponse from(Requirement r) {
        return RequirementResponse.builder()
                .id(r.getId())
                .projectId(r.getProject().getId())
                .reqKey(r.getReqKey())
                .title(r.getTitle())
                .description(r.getDescription())
                .type(r.getType())
                .priority(r.getPriority())
                .status(r.getStatus())
                .parentRequirementId(r.getParentRequirement() != null ? r.getParentRequirement().getId() : null)
                .parentRequirementKey(r.getParentRequirement() != null ? r.getParentRequirement().getReqKey() : null)
                .createdById(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null)
                .createdByName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null)
                .assignedToId(r.getAssignedTo() != null ? r.getAssignedTo().getId() : null)
                .assignedToName(r.getAssignedTo() != null ? r.getAssignedTo().getFullName() : null)
                .dueDate(r.getDueDate())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
