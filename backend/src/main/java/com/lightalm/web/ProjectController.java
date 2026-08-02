package com.lightalm.web;

import com.lightalm.dto.CreateProjectRequest;
import com.lightalm.dto.GithubIntegrationRequest;
import com.lightalm.dto.JenkinsIntegrationRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.ProjectResponse;
import com.lightalm.dto.UpdateProjectRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public PageResponse<ProjectResponse> list(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return projectService.list(principal, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.create(request, principal);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.get(projectId, principal);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.update(projectId, request, principal);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        projectService.delete(projectId, principal);
    }

    @PutMapping("/{projectId}/integrations/github")
    public ProjectResponse updateGithubIntegration(@PathVariable Long projectId,
                                                     @Valid @RequestBody GithubIntegrationRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.updateGithubIntegration(projectId, request, principal);
    }

    @PutMapping("/{projectId}/integrations/jenkins")
    public ProjectResponse updateJenkinsIntegration(@PathVariable Long projectId,
                                                      @Valid @RequestBody JenkinsIntegrationRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.updateJenkinsIntegration(projectId, request, principal);
    }
}
