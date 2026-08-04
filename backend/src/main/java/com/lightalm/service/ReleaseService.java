package com.lightalm.service;

import com.lightalm.domain.Issue;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Release;
import com.lightalm.domain.ReleaseItem;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.AddReleaseItemRequest;
import com.lightalm.dto.ChangeReleaseStatusRequest;
import com.lightalm.dto.CreateReleaseRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.ReleaseItemResponse;
import com.lightalm.dto.ReleaseNotesResponse;
import com.lightalm.dto.ReleaseResponse;
import com.lightalm.dto.UpdateReleaseRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.ReleaseItemRepository;
import com.lightalm.repository.ReleaseRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseItemRepository releaseItemRepository;
    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public PageResponse<ReleaseResponse> list(Long projectId, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return PageResponse.from(releaseRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId), pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ReleaseResponse get(Long projectId, Long releaseId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return toResponse(getEntity(projectId, releaseId));
    }

    @Transactional
    public ReleaseResponse create(Long projectId, CreateReleaseRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        if (releaseRepository.existsByProjectIdAndVersion(projectId, request.getVersion())) {
            throw new ValidationException("이미 사용 중인 version입니다: " + request.getVersion());
        }
        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        Release release = Release.builder()
                .project(project)
                .version(request.getVersion())
                .name(request.getName())
                .releaseDate(request.getReleaseDate())
                .description(request.getDescription())
                .createdBy(creator)
                .build();
        return toResponse(releaseRepository.save(release));
    }

    @Transactional
    public ReleaseResponse update(Long projectId, Long releaseId, UpdateReleaseRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Release release = getEntity(projectId, releaseId);
        release.setVersion(request.getVersion());
        release.setName(request.getName());
        release.setReleaseDate(request.getReleaseDate());
        release.setDescription(request.getDescription());
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse changeStatus(Long projectId, Long releaseId, ChangeReleaseStatusRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        Release release = getEntity(projectId, releaseId);
        release.setStatus(request.getStatus());
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse addItem(Long projectId, Long releaseId, AddReleaseItemRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Release release = getEntity(projectId, releaseId);
        TargetType targetType = request.getTargetType();
        if (targetType != TargetType.REQUIREMENT && targetType != TargetType.ISSUE) {
            throw new ValidationException("릴리스에는 REQUIREMENT 또는 ISSUE만 추가할 수 있습니다.");
        }
        validateTargetExists(projectId, targetType, request.getTargetId());
        if (releaseItemRepository.existsByReleaseIdAndTargetTypeAndTargetId(releaseId, targetType, request.getTargetId())) {
            throw new ValidationException("이미 릴리스에 포함된 항목입니다.");
        }
        User adder = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        ReleaseItem item = ReleaseItem.builder()
                .release(release)
                .targetType(targetType)
                .targetId(request.getTargetId())
                .addedBy(adder)
                .build();
        releaseItemRepository.save(item);
        return toResponse(release);
    }

    @Transactional
    public void removeItem(Long projectId, Long releaseId, Long itemId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        getEntity(projectId, releaseId);
        ReleaseItem item = releaseItemRepository.findByIdAndReleaseId(itemId, releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("릴리스 항목을 찾을 수 없습니다: " + itemId));
        releaseItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public ReleaseNotesResponse generateNotes(Long projectId, Long releaseId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Release release = getEntity(projectId, releaseId);
        List<ReleaseItem> items = releaseItemRepository.findByReleaseId(releaseId);

        StringBuilder sb = new StringBuilder();
        sb.append("# Release ").append(release.getVersion());
        if (release.getName() != null && !release.getName().isBlank()) {
            sb.append(" — ").append(release.getName());
        }
        sb.append("\n\n");

        List<ReleaseItem> requirementItems = items.stream().filter(i -> i.getTargetType() == TargetType.REQUIREMENT).toList();
        List<ReleaseItem> issueItems = items.stream().filter(i -> i.getTargetType() == TargetType.ISSUE).toList();

        sb.append("## 요구사항\n");
        if (requirementItems.isEmpty()) {
            sb.append("- (없음)\n");
        } else {
            for (ReleaseItem item : requirementItems) {
                String[] keyTitle = resolveKeyAndTitle(item);
                sb.append("- ").append(keyTitle[0]).append(": ").append(keyTitle[1]).append("\n");
            }
        }
        sb.append("\n## 이슈\n");
        if (issueItems.isEmpty()) {
            sb.append("- (없음)\n");
        } else {
            for (ReleaseItem item : issueItems) {
                String[] keyTitle = resolveKeyAndTitle(item);
                sb.append("- ").append(keyTitle[0]).append(": ").append(keyTitle[1]).append("\n");
            }
        }

        return ReleaseNotesResponse.builder()
                .releaseId(release.getId())
                .version(release.getVersion())
                .notes(sb.toString())
                .build();
    }

    private void validateTargetExists(Long projectId, TargetType targetType, Long targetId) {
        if (targetType == TargetType.REQUIREMENT) {
            Requirement requirement = requirementRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId));
            if (!requirement.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId);
            }
        } else {
            Issue issue = issueRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId));
            if (!issue.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId);
            }
        }
    }

    /** index 0 = key, 1 = title, 2 = status (null-safe — "(삭제됨)"/null if the referenced row is gone). */
    private String[] resolveKeyAndTitle(ReleaseItem item) {
        if (item.getTargetType() == TargetType.REQUIREMENT) {
            return requirementRepository.findById(item.getTargetId())
                    .map(r -> new String[] {r.getReqKey(), r.getTitle(), r.getStatus().name()})
                    .orElse(new String[] {"(삭제됨)", "(삭제됨)", null});
        }
        return issueRepository.findById(item.getTargetId())
                .map(i -> new String[] {i.getIssueKey(), i.getTitle(), i.getStatus().name()})
                .orElse(new String[] {"(삭제됨)", "(삭제됨)", null});
    }

    private ReleaseResponse toResponse(Release release) {
        List<ReleaseItemResponse> items = releaseItemRepository.findByReleaseId(release.getId()).stream()
                .map(item -> {
                    String[] keyTitleStatus = resolveKeyAndTitle(item);
                    return ReleaseItemResponse.from(item, keyTitleStatus[0], keyTitleStatus[1], keyTitleStatus[2]);
                })
                .collect(Collectors.toList());
        return ReleaseResponse.from(release, items);
    }

    Release getEntity(Long projectId, Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ResourceNotFoundException("릴리스를 찾을 수 없습니다: " + releaseId));
        if (!release.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("릴리스를 찾을 수 없습니다: " + releaseId);
        }
        return release;
    }
}
