package com.lightalm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_key", nullable = false, unique = true, length = 10)
    private String projectKey;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "issue_seq", nullable = false)
    @Builder.Default
    private Integer issueSeq = 0;

    @Column(name = "requirement_seq", nullable = false)
    @Builder.Default
    private Integer requirementSeq = 0;

    @Column(name = "test_case_seq", nullable = false)
    @Builder.Default
    private Integer testCaseSeq = 0;

    @Column(name = "github_repo_owner", length = 100)
    private String githubRepoOwner;

    @Column(name = "github_repo_name", length = 100)
    private String githubRepoName;

    @Column(name = "github_access_token", length = 255)
    private String githubAccessToken;

    @Column(name = "github_webhook_secret", length = 255)
    private String githubWebhookSecret;

    @Column(name = "jenkins_base_url", length = 255)
    private String jenkinsBaseUrl;

    @Column(name = "jenkins_job_name", length = 150)
    private String jenkinsJobName;

    @Column(name = "jenkins_api_user", length = 100)
    private String jenkinsApiUser;

    @Column(name = "jenkins_api_token", length = 255)
    private String jenkinsApiToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
