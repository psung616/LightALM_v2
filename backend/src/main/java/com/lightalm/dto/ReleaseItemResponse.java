package com.lightalm.dto;

import com.lightalm.domain.ReleaseItem;
import com.lightalm.domain.TargetType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReleaseItemResponse {
    private Long id;
    private TargetType targetType;
    private Long targetId;
    private String targetKey;
    private String targetTitle;
    private String targetStatus;
    private Long addedById;
    private String addedByName;
    private LocalDateTime addedAt;

    public static ReleaseItemResponse from(ReleaseItem item, String targetKey, String targetTitle, String targetStatus) {
        return ReleaseItemResponse.builder()
                .id(item.getId())
                .targetType(item.getTargetType())
                .targetId(item.getTargetId())
                .targetKey(targetKey)
                .targetTitle(targetTitle)
                .targetStatus(targetStatus)
                .addedById(item.getAddedBy() != null ? item.getAddedBy().getId() : null)
                .addedByName(item.getAddedBy() != null ? item.getAddedBy().getFullName() : null)
                .addedAt(item.getAddedAt())
                .build();
    }
}
