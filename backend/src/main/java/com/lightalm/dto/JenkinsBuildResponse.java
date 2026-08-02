package com.lightalm.dto;

import com.lightalm.domain.BuildStatus;
import com.lightalm.domain.JenkinsBuild;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JenkinsBuildResponse {
    private Long id;
    private String jobName;
    private Integer buildNumber;
    private BuildStatus status;
    private String buildUrl;
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public static JenkinsBuildResponse from(JenkinsBuild build) {
        return JenkinsBuildResponse.builder()
                .id(build.getId())
                .jobName(build.getJobName())
                .buildNumber(build.getBuildNumber())
                .status(build.getStatus())
                .buildUrl(build.getBuildUrl())
                .triggeredBy(build.getTriggeredBy())
                .startedAt(build.getStartedAt())
                .finishedAt(build.getFinishedAt())
                .createdAt(build.getCreatedAt())
                .build();
    }
}
