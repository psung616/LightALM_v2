package com.lightalm.service;

import com.lightalm.domain.JenkinsBuild;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.dto.JenkinsBuildResponse;
import com.lightalm.dto.JenkinsWebhookPayload;
import com.lightalm.dto.TriggerBuildRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.integration.jenkins.JenkinsApiClient;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.JenkinsBuildRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.security.UserPrincipal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JenkinsBuildService {

    private final JenkinsBuildRepository jenkinsBuildRepository;
    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final JenkinsApiClient jenkinsApiClient;

    @Transactional(readOnly = true)
    public List<JenkinsBuildResponse> list(Long projectId, TargetType targetType, Long targetId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        verifyTargetExists(projectId, targetType, targetId);
        return jenkinsBuildRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId).stream()
                .map(JenkinsBuildResponse::from)
                .toList();
    }

    @Transactional
    public void triggerBuild(Long projectId, TriggerBuildRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        verifyTargetExists(projectId, request.getTargetType(), request.getTargetId());
        Project project = projectService.getEntity(projectId);
        if (project.getJenkinsBaseUrl() == null || project.getJenkinsJobName() == null) {
            throw new ValidationException("프로젝트에 Jenkins 연동 정보(baseUrl/jobName)가 설정되어 있지 않습니다.");
        }
        jenkinsApiClient.triggerBuild(project.getJenkinsBaseUrl(), project.getJenkinsJobName(),
                project.getJenkinsApiUser(), project.getJenkinsApiToken());
    }

    public boolean verifyWebhookToken(Long projectId, String token) {
        Project project = projectService.getEntity(projectId);
        return token != null && !token.isBlank() && Objects.equals(token, project.getGithubWebhookSecret());
    }

    @Transactional
    public void handleWebhook(Long projectId, JenkinsWebhookPayload payload) {
        if (payload.getJobName() == null || payload.getBuildNumber() == null) {
            throw new ValidationException("jobName과 buildNumber는 필수입니다.");
        }
        Project project = projectService.getEntity(projectId);

        JenkinsBuild build = jenkinsBuildRepository.findByProjectIdAndJobNameAndBuildNumber(
                projectId, payload.getJobName(), payload.getBuildNumber()).orElse(null);

        if (build == null) {
            build = JenkinsBuild.builder()
                    .project(project)
                    .targetType(payload.getTargetType())
                    .targetId(payload.getTargetId())
                    .jobName(payload.getJobName())
                    .buildNumber(payload.getBuildNumber())
                    .status(payload.getStatus())
                    .buildUrl(payload.getBuildUrl())
                    .triggeredBy(payload.getTriggeredBy())
                    .startedAt(payload.getStartedAt())
                    .finishedAt(payload.getFinishedAt())
                    .build();
        } else {
            build.setStatus(payload.getStatus());
            build.setBuildUrl(payload.getBuildUrl());
            build.setTriggeredBy(payload.getTriggeredBy());
            if (payload.getStartedAt() != null) {
                build.setStartedAt(payload.getStartedAt());
            }
            if (payload.getFinishedAt() != null) {
                build.setFinishedAt(payload.getFinishedAt());
            }
            if (payload.getTargetType() != null) {
                build.setTargetType(payload.getTargetType());
            }
            if (payload.getTargetId() != null) {
                build.setTargetId(payload.getTargetId());
            }
        }
        jenkinsBuildRepository.save(build);
    }

    private void verifyTargetExists(Long projectId, TargetType targetType, Long targetId) {
        if (targetType == TargetType.REQUIREMENT) {
            var requirement = requirementRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId));
            if (!requirement.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId);
            }
        } else {
            var issue = issueRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId));
            if (!issue.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId);
            }
        }
    }
}
