package com.lightalm.dto;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectResponse {
    private Long id;
    private String projectKey;
    private String name;
    private String description;
    private ProjectStatus status;
    private String githubRepoOwner;
    private String githubRepoName;
    private String githubAccessTokenMasked;
    private String githubWebhookSecretMasked;
    private String jenkinsBaseUrl;
    private String jenkinsJobName;
    private String jenkinsApiUser;
    private String jenkinsApiTokenMasked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectKey(project.getProjectKey())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .githubRepoOwner(project.getGithubRepoOwner())
                .githubRepoName(project.getGithubRepoName())
                .githubAccessTokenMasked(mask(project.getGithubAccessToken()))
                .githubWebhookSecretMasked(mask(project.getGithubWebhookSecret()))
                .jenkinsBaseUrl(project.getJenkinsBaseUrl())
                .jenkinsJobName(project.getJenkinsJobName())
                .jenkinsApiUser(project.getJenkinsApiUser())
                .jenkinsApiTokenMasked(mask(project.getJenkinsApiToken()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private static String mask(String value) {
        return (value == null || value.isBlank()) ? null : "****";
    }
}
