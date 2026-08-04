package com.lightalm.dto;

import com.lightalm.domain.TestRun;
import com.lightalm.domain.TestRunResult;
import com.lightalm.domain.TestRunStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestRunResponse {
    private Long id;
    private Long projectId;
    private Long releaseId;
    private String releaseVersion;
    private String name;
    private TestRunStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<TestRunResultResponse> results;

    public static TestRunResponse from(TestRun run, List<TestRunResult> results) {
        return TestRunResponse.builder()
                .id(run.getId())
                .projectId(run.getProject().getId())
                .releaseId(run.getRelease() != null ? run.getRelease().getId() : null)
                .releaseVersion(run.getRelease() != null ? run.getRelease().getVersion() : null)
                .name(run.getName())
                .status(run.getStatus())
                .createdById(run.getCreatedBy() != null ? run.getCreatedBy().getId() : null)
                .createdByName(run.getCreatedBy() != null ? run.getCreatedBy().getFullName() : null)
                .createdAt(run.getCreatedAt())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt())
                .results(results.stream().map(TestRunResultResponse::from).toList())
                .build();
    }
}
