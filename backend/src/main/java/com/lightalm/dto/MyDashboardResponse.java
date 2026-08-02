package com.lightalm.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyDashboardResponse {
    private Map<String, Long> assignedIssuesByStatus;
    private Map<String, Long> assignedRequirementsByStatus;
    private List<AssignedItem> overdue;
    private List<AssignedItem> dueSoon;
    private List<ProjectSummary> byProject;

    @Getter
    @Builder
    public static class AssignedItem {
        private String type;
        private Long id;
        private String key;
        private String title;
        private String projectKey;
        private LocalDate dueDate;
        private String status;
    }

    @Getter
    @Builder
    public static class ProjectSummary {
        private Long projectId;
        private String projectKey;
        private String projectName;
        private long assignedIssueCount;
        private long assignedRequirementCount;
    }
}
