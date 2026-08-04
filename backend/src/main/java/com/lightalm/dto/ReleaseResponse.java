package com.lightalm.dto;

import com.lightalm.domain.Release;
import com.lightalm.domain.ReleaseStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReleaseResponse {
    private Long id;
    private Long projectId;
    private String version;
    private String name;
    private ReleaseStatus status;
    private LocalDate releaseDate;
    private String description;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReleaseItemResponse> items;

    public static ReleaseResponse from(Release release, List<ReleaseItemResponse> items) {
        return ReleaseResponse.builder()
                .id(release.getId())
                .projectId(release.getProject().getId())
                .version(release.getVersion())
                .name(release.getName())
                .status(release.getStatus())
                .releaseDate(release.getReleaseDate())
                .description(release.getDescription())
                .createdById(release.getCreatedBy() != null ? release.getCreatedBy().getId() : null)
                .createdByName(release.getCreatedBy() != null ? release.getCreatedBy().getFullName() : null)
                .createdAt(release.getCreatedAt())
                .updatedAt(release.getUpdatedAt())
                .items(items)
                .build();
    }
}
