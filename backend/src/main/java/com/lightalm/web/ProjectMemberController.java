package com.lightalm.web;

import com.lightalm.dto.AddMemberRequest;
import com.lightalm.dto.ProjectMemberResponse;
import com.lightalm.dto.UpdateMemberRoleRequest;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.ProjectMemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping
    public List<ProjectMemberResponse> list(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return projectMemberService.list(projectId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse add(@PathVariable Long projectId, @Valid @RequestBody AddMemberRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return projectMemberService.addMember(projectId, request, principal);
    }

    @PutMapping("/{userId}")
    public ProjectMemberResponse updateRole(@PathVariable Long projectId, @PathVariable Long userId,
                                             @Valid @RequestBody UpdateMemberRoleRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return projectMemberService.updateRole(projectId, userId, request, principal);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long projectId, @PathVariable Long userId, @AuthenticationPrincipal UserPrincipal principal) {
        projectMemberService.removeMember(projectId, userId, principal);
    }
}
