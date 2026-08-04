package com.lightalm.dto;

import com.lightalm.domain.Priority;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TestCaseStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestCaseResponse {
    private Long id;
    private Long projectId;
    private Long requirementId;
    private String requirementKey;
    private String tcKey;
    private String title;
    private String description;
    private String preconditions;
    private String steps;
    private String expectedResult;
    private Priority priority;
    private TestCaseStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TestCaseResponse from(TestCase tc) {
        return TestCaseResponse.builder()
                .id(tc.getId())
                .projectId(tc.getProject().getId())
                .requirementId(tc.getRequirement() != null ? tc.getRequirement().getId() : null)
                .requirementKey(tc.getRequirement() != null ? tc.getRequirement().getReqKey() : null)
                .tcKey(tc.getTcKey())
                .title(tc.getTitle())
                .description(tc.getDescription())
                .preconditions(tc.getPreconditions())
                .steps(tc.getSteps())
                .expectedResult(tc.getExpectedResult())
                .priority(tc.getPriority())
                .status(tc.getStatus())
                .createdById(tc.getCreatedBy() != null ? tc.getCreatedBy().getId() : null)
                .createdByName(tc.getCreatedBy() != null ? tc.getCreatedBy().getFullName() : null)
                .createdAt(tc.getCreatedAt())
                .updatedAt(tc.getUpdatedAt())
                .build();
    }
}
