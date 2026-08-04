package com.lightalm.dto;

import com.lightalm.domain.AuditAction;
import com.lightalm.domain.AuditLog;
import com.lightalm.domain.AuditTargetType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private Long projectId;
    private AuditTargetType targetType;
    private Long targetId;
    private AuditAction action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long actorId;
    private String actorName;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .projectId(log.getProject() != null ? log.getProject().getId() : null)
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .action(log.getAction())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorName(log.getActor() != null ? log.getActor().getFullName() : null)
                .createdAt(log.getCreatedAt())
                .build();
    }
}
