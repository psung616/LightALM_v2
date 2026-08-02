package com.lightalm.service;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectMember;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.User;
import com.lightalm.dto.AddMemberRequest;
import com.lightalm.dto.ProjectMemberResponse;
import com.lightalm.dto.UpdateMemberRoleRequest;
import com.lightalm.exception.ForbiddenException;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.ProjectMemberRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<ProjectRole> findRole(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId).map(ProjectMember::getRole);
    }

    /**
     * 시스템 ADMIN은 모든 프로젝트에 대해 항상 PROJECT_ADMIN 이상 권한을 가진 것으로 취급한다(§6).
     */
    public void requireRole(Long projectId, UserPrincipal principal, ProjectRole minRole) {
        if (principal.isAdmin()) {
            return;
        }
        ProjectRole role = findRole(projectId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("해당 프로젝트에 대한 접근 권한이 없습니다."));
        if (!role.isAtLeast(minRole)) {
            throw new ForbiddenException("해당 작업을 수행할 권한이 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(Long projectId, UserPrincipal principal) {
        requireRole(projectId, principal, ProjectRole.VIEWER);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(Long projectId, AddMemberRequest request, UserPrincipal principal) {
        requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new ValidationException("이미 프로젝트 멤버입니다.");
        }
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(request.getRole())
                .build();
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public ProjectMemberResponse updateRole(Long projectId, Long userId, UpdateMemberRoleRequest request, UserPrincipal principal) {
        requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트 멤버를 찾을 수 없습니다."));
        member.setRole(request.getRole());
        return ProjectMemberResponse.from(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, UserPrincipal principal) {
        requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResourceNotFoundException("프로젝트 멤버를 찾을 수 없습니다.");
        }
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Transactional
    public void registerProjectAdmin(Project project, User user) {
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectRole.PROJECT_ADMIN)
                .build();
        projectMemberRepository.save(member);
    }
}
