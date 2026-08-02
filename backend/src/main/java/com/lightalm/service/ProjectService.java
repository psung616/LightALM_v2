package com.lightalm.service;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateProjectRequest;
import com.lightalm.dto.GithubIntegrationRequest;
import com.lightalm.dto.JenkinsIntegrationRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.ProjectResponse;
import com.lightalm.dto.UpdateProjectRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(UserPrincipal principal, Pageable pageable) {
        if (principal.isAdmin()) {
            return PageResponse.from(projectRepository.findAll(pageable).map(ProjectResponse::from));
        }
        return PageResponse.from(projectRepository.findByMemberUserId(principal.getId(), pageable).map(ProjectResponse::from));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id, UserPrincipal principal) {
        projectMemberService.requireRole(id, principal, ProjectRole.VIEWER);
        return ProjectResponse.from(getEntity(id));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, UserPrincipal principal) {
        if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new ValidationException("이미 사용 중인 projectKey입니다: " + request.getProjectKey());
        }
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        Project project = Project.builder()
                .projectKey(request.getProjectKey())
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();
        Project saved = projectRepository.save(project);
        projectMemberService.registerProjectAdmin(saved, creator);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(id, principal, ProjectRole.PROJECT_ADMIN);
        Project project = getEntity(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        projectMemberService.requireRole(id, principal, ProjectRole.PROJECT_ADMIN);
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("프로젝트를 찾을 수 없습니다: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Transactional
    public ProjectResponse updateGithubIntegration(Long id, GithubIntegrationRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(id, principal, ProjectRole.PROJECT_ADMIN);
        Project project = getEntity(id);
        project.setGithubRepoOwner(request.getRepoOwner());
        project.setGithubRepoName(request.getRepoName());
        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            project.setGithubAccessToken(request.getAccessToken());
        }
        if (request.getWebhookSecret() != null && !request.getWebhookSecret().isBlank()) {
            project.setGithubWebhookSecret(request.getWebhookSecret());
        }
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateJenkinsIntegration(Long id, JenkinsIntegrationRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(id, principal, ProjectRole.PROJECT_ADMIN);
        Project project = getEntity(id);
        project.setJenkinsBaseUrl(request.getBaseUrl());
        project.setJenkinsJobName(request.getJobName());
        if (request.getApiUser() != null && !request.getApiUser().isBlank()) {
            project.setJenkinsApiUser(request.getApiUser());
        }
        if (request.getApiToken() != null && !request.getApiToken().isBlank()) {
            project.setJenkinsApiToken(request.getApiToken());
        }
        return ProjectResponse.from(project);
    }

    /**
     * 요구사항 키 채번(§8 Phase 3/4). 동시성 보호를 위해 프로젝트 행에 비관적 락을 건 뒤 증가시킨다.
     */
    @Transactional
    public String nextRequirementKey(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
        int next = project.getRequirementSeq() + 1;
        project.setRequirementSeq(next);
        return project.getProjectKey() + "-R" + next;
    }

    /**
     * 이슈 키 채번(§8 Phase 3/5). 동시성 보호를 위해 프로젝트 행에 비관적 락을 건 뒤 증가시킨다.
     */
    @Transactional
    public String nextIssueKey(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
        int next = project.getIssueSeq() + 1;
        project.setIssueSeq(next);
        return project.getProjectKey() + "-" + next;
    }

    @Transactional(readOnly = true)
    public Project getEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트를 찾을 수 없습니다: " + id));
    }
}
