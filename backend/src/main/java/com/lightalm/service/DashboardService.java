package com.lightalm.service;

import com.lightalm.domain.Issue;
import com.lightalm.domain.IssueStatus;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectMember;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementStatus;
import com.lightalm.dto.IssueResponse;
import com.lightalm.dto.MyDashboardResponse;
import com.lightalm.dto.ProjectDashboardSummaryResponse;
import com.lightalm.dto.RequirementResponse;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.ProjectMemberRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.security.UserPrincipal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<IssueStatus> ISSUE_TERMINAL_STATUSES = EnumSet.of(IssueStatus.DONE, IssueStatus.CLOSED);
    private static final Set<RequirementStatus> REQUIREMENT_TERMINAL_STATUSES =
            EnumSet.of(RequirementStatus.IMPLEMENTED, RequirementStatus.VERIFIED, RequirementStatus.REJECTED);

    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public ProjectDashboardSummaryResponse projectSummary(Long projectId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);

        Map<String, Long> requirementCounts = new LinkedHashMap<>();
        for (RequirementStatus status : RequirementStatus.values()) {
            requirementCounts.put(status.name(), requirementRepository.countByProjectIdAndStatus(projectId, status));
        }
        Map<String, Long> issueCounts = new LinkedHashMap<>();
        for (IssueStatus status : IssueStatus.values()) {
            issueCounts.put(status.name(), issueRepository.countByProjectIdAndStatus(projectId, status));
        }

        List<RequirementResponse> recentRequirements = requirementRepository.findTop10ByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream().map(RequirementResponse::from).toList();
        List<IssueResponse> recentIssues = issueRepository.findTop10ByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream().map(IssueResponse::from).toList();

        return ProjectDashboardSummaryResponse.builder()
                .requirementCountsByStatus(requirementCounts)
                .issueCountsByStatus(issueCounts)
                .recentRequirements(recentRequirements)
                .recentIssues(recentIssues)
                .build();
    }

    @Transactional(readOnly = true)
    public MyDashboardResponse myDashboard(UserPrincipal principal) {
        List<ProjectMember> memberships = projectMemberRepository.findByUserId(principal.getId());
        List<Long> projectIds = memberships.stream().map(m -> m.getProject().getId()).distinct().toList();

        if (projectIds.isEmpty()) {
            return MyDashboardResponse.builder()
                    .assignedIssuesByStatus(emptyIssueCounts())
                    .assignedRequirementsByStatus(emptyRequirementCounts())
                    .overdue(List.of())
                    .dueSoon(List.of())
                    .byProject(List.of())
                    .build();
        }

        List<Issue> assignedIssues = issueRepository.findByProjectIdInAndAssigneeId(projectIds, principal.getId());
        List<Requirement> assignedRequirements = requirementRepository.findByProjectIdInAndAssignedToId(projectIds, principal.getId());

        Map<String, Long> issuesByStatus = emptyIssueCounts();
        assignedIssues.forEach(i -> issuesByStatus.merge(i.getStatus().name(), 1L, Long::sum));

        Map<String, Long> requirementsByStatus = emptyRequirementCounts();
        assignedRequirements.forEach(r -> requirementsByStatus.merge(r.getStatus().name(), 1L, Long::sum));

        LocalDate today = LocalDate.now();
        LocalDate soonThreshold = today.plusDays(7);

        List<MyDashboardResponse.AssignedItem> overdue = new java.util.ArrayList<>();
        List<MyDashboardResponse.AssignedItem> dueSoon = new java.util.ArrayList<>();

        for (Issue issue : assignedIssues) {
            if (issue.getDueDate() == null || ISSUE_TERMINAL_STATUSES.contains(issue.getStatus())) {
                continue;
            }
            MyDashboardResponse.AssignedItem item = MyDashboardResponse.AssignedItem.builder()
                    .type("ISSUE")
                    .id(issue.getId())
                    .key(issue.getIssueKey())
                    .title(issue.getTitle())
                    .projectKey(issue.getProject().getProjectKey())
                    .dueDate(issue.getDueDate())
                    .status(issue.getStatus().name())
                    .build();
            if (issue.getDueDate().isBefore(today)) {
                overdue.add(item);
            } else if (!issue.getDueDate().isAfter(soonThreshold)) {
                dueSoon.add(item);
            }
        }

        for (Requirement requirement : assignedRequirements) {
            if (requirement.getDueDate() == null || REQUIREMENT_TERMINAL_STATUSES.contains(requirement.getStatus())) {
                continue;
            }
            MyDashboardResponse.AssignedItem item = MyDashboardResponse.AssignedItem.builder()
                    .type("REQUIREMENT")
                    .id(requirement.getId())
                    .key(requirement.getReqKey())
                    .title(requirement.getTitle())
                    .projectKey(requirement.getProject().getProjectKey())
                    .dueDate(requirement.getDueDate())
                    .status(requirement.getStatus().name())
                    .build();
            if (requirement.getDueDate().isBefore(today)) {
                overdue.add(item);
            } else if (!requirement.getDueDate().isAfter(soonThreshold)) {
                dueSoon.add(item);
            }
        }

        Map<Long, Long> issueCountByProject = assignedIssues.stream()
                .collect(Collectors.groupingBy(i -> i.getProject().getId(), Collectors.counting()));
        Map<Long, Long> requirementCountByProject = assignedRequirements.stream()
                .collect(Collectors.groupingBy(r -> r.getProject().getId(), Collectors.counting()));

        Map<Long, Project> projectsById = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        List<MyDashboardResponse.ProjectSummary> byProject = projectIds.stream()
                .map(id -> {
                    Project project = projectsById.get(id);
                    return MyDashboardResponse.ProjectSummary.builder()
                            .projectId(id)
                            .projectKey(project != null ? project.getProjectKey() : null)
                            .projectName(project != null ? project.getName() : null)
                            .assignedIssueCount(issueCountByProject.getOrDefault(id, 0L))
                            .assignedRequirementCount(requirementCountByProject.getOrDefault(id, 0L))
                            .build();
                })
                .toList();

        return MyDashboardResponse.builder()
                .assignedIssuesByStatus(issuesByStatus)
                .assignedRequirementsByStatus(requirementsByStatus)
                .overdue(overdue)
                .dueSoon(dueSoon)
                .byProject(byProject)
                .build();
    }

    private Map<String, Long> emptyIssueCounts() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (IssueStatus status : IssueStatus.values()) {
            map.put(status.name(), 0L);
        }
        return map;
    }

    private Map<String, Long> emptyRequirementCounts() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (RequirementStatus status : RequirementStatus.values()) {
            map.put(status.name(), 0L);
        }
        return map;
    }
}
