package com.lightalm.dto;

import com.lightalm.domain.TestResult;
import com.lightalm.domain.TestRunResult;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestRunResultResponse {
    private Long id;
    private Long testCaseId;
    private String tcKey;
    private String testCaseTitle;
    private TestResult result;
    private String actualResult;
    private Long executedById;
    private String executedByName;
    private LocalDateTime executedAt;

    public static TestRunResultResponse from(TestRunResult r) {
        return TestRunResultResponse.builder()
                .id(r.getId())
                .testCaseId(r.getTestCase().getId())
                .tcKey(r.getTestCase().getTcKey())
                .testCaseTitle(r.getTestCase().getTitle())
                .result(r.getResult())
                .actualResult(r.getActualResult())
                .executedById(r.getExecutedBy() != null ? r.getExecutedBy().getId() : null)
                .executedByName(r.getExecutedBy() != null ? r.getExecutedBy().getFullName() : null)
                .executedAt(r.getExecutedAt())
                .build();
    }
}
