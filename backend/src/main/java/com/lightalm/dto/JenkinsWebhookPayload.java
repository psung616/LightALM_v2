package com.lightalm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightalm.domain.BuildStatus;
import com.lightalm.domain.TargetType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JenkinsWebhookPayload {
    private TargetType targetType;
    private Long targetId;
    private String jobName;
    private Integer buildNumber;
    private BuildStatus status;
    private String buildUrl;
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
