package com.lightalm.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectDashboardSummaryResponse {
    private Map<String, Long> requirementCountsByStatus;
    private Map<String, Long> issueCountsByStatus;
    private List<RequirementResponse> recentRequirements;
    private List<IssueResponse> recentIssues;
}
