package com.lightalm.service;

import com.lightalm.domain.Issue;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TraceabilityLink;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTraceabilityLinkRequest;
import com.lightalm.dto.RequirementLinkResponse;
import com.lightalm.dto.TraceabilityLinkResponse;
import com.lightalm.dto.TraceabilityMatrixResponse;
import com.lightalm.dto.TraceabilityTreeResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TraceabilityLinkRepository;
import com.lightalm.repository.TraceabilityTreeRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraceabilityService {

    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final TraceabilityTreeRepository traceabilityTreeRepository;
    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public TraceabilityMatrixResponse matrix(Long projectId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);

        List<TraceabilityMatrixResponse.RequirementBrief> requirements = requirementRepository.findByProjectId(projectId).stream()
                .map(r -> TraceabilityMatrixResponse.RequirementBrief.builder()
                        .id(r.getId()).reqKey(r.getReqKey()).title(r.getTitle()).build())
                .toList();

        List<TraceabilityMatrixResponse.IssueBrief> issues = issueRepository.findByProjectId(projectId).stream()
                .map(i -> TraceabilityMatrixResponse.IssueBrief.builder()
                        .id(i.getId()).issueKey(i.getIssueKey()).title(i.getTitle()).build())
                .toList();

        List<TraceabilityLinkResponse> links = traceabilityLinkRepository.findByProjectId(projectId).stream()
                .filter(l -> isRequirementIssuePair(l.getSourceType(), l.getTargetType()))
                .map(TraceabilityLinkResponse::from)
                .toList();

        return TraceabilityMatrixResponse.builder()
                .requirements(requirements)
                .issues(issues)
                .links(links)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RequirementLinkResponse> requirementLinks(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Requirement requirement = requirementRepository.findById(reqId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId));
        if (!requirement.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId);
        }

        List<TraceabilityLink> outgoing = traceabilityLinkRepository.findBySourceTypeAndSourceId(TargetType.REQUIREMENT, reqId);
        List<TraceabilityLink> incoming = traceabilityLinkRepository.findByTargetTypeAndTargetId(TargetType.REQUIREMENT, reqId);

        List<RequirementLinkResponse> result = new ArrayList<>();
        for (TraceabilityLink link : outgoing) {
            result.add(toRequirementLinkResponse(link, link.getTargetType(), link.getTargetId(), link.getLinkType()));
        }
        for (TraceabilityLink link : incoming) {
            result.add(toRequirementLinkResponse(link, link.getSourceType(), link.getSourceId(), link.getLinkType()));
        }
        return result;
    }

    private RequirementLinkResponse toRequirementLinkResponse(TraceabilityLink link, TargetType otherType, Long otherId,
                                                                com.lightalm.domain.LinkType linkType) {
        if (otherType == TargetType.ISSUE) {
            Issue issue = issueRepository.findById(otherId).orElse(null);
            return RequirementLinkResponse.builder()
                    .linkId(link.getId())
                    .linkedType(TargetType.ISSUE)
                    .linkedId(otherId)
                    .linkedKey(issue != null ? issue.getIssueKey() : null)
                    .linkedTitle(issue != null ? issue.getTitle() : null)
                    .linkedStatus(issue != null ? issue.getStatus().name() : null)
                    .linkType(linkType)
                    .build();
        }
        Requirement requirement = requirementRepository.findById(otherId).orElse(null);
        return RequirementLinkResponse.builder()
                .linkId(link.getId())
                .linkedType(TargetType.REQUIREMENT)
                .linkedId(otherId)
                .linkedKey(requirement != null ? requirement.getReqKey() : null)
                .linkedTitle(requirement != null ? requirement.getTitle() : null)
                .linkedStatus(requirement != null ? requirement.getStatus().name() : null)
                .linkType(linkType)
                .build();
    }

    @Transactional
    public TraceabilityLinkResponse createLink(Long projectId, CreateTraceabilityLinkRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);

        validateTarget(projectId, request.getSourceType(), request.getSourceId());
        validateTarget(projectId, request.getTargetType(), request.getTargetId());

        if (traceabilityLinkRepository.existsBySourceTypeAndSourceIdAndTargetTypeAndTargetIdAndLinkType(
                request.getSourceType(), request.getSourceId(), request.getTargetType(), request.getTargetId(), request.getLinkType())) {
            throw new ValidationException("이미 동일한 링크가 존재합니다.");
        }

        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));

        TraceabilityLink link = TraceabilityLink.builder()
                .project(project)
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .linkType(request.getLinkType())
                .createdBy(creator)
                .build();
        return TraceabilityLinkResponse.from(traceabilityLinkRepository.save(link));
    }

    @Transactional
    public void deleteLink(Long projectId, Long linkId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        TraceabilityLink link = traceabilityLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("링크를 찾을 수 없습니다: " + linkId));
        if (!link.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("링크를 찾을 수 없습니다: " + linkId);
        }
        traceabilityLinkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public TraceabilityTreeResponse tree(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Requirement self = requirementRepository.findById(reqId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId));
        if (!self.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId);
        }

        List<TraceabilityTreeResponse.AncestorNode> ancestors = traceabilityTreeRepository.findAncestors(reqId).stream()
                .map(row -> TraceabilityTreeResponse.AncestorNode.builder()
                        .id(((Number) row.get("id")).longValue())
                        .reqKey((String) row.get("req_key"))
                        .title((String) row.get("title"))
                        .build())
                .toList();

        List<Map<String, Object>> descendantRows = traceabilityTreeRepository.findDescendants(reqId);

        List<Long> allNodeIds = new ArrayList<>();
        allNodeIds.add(reqId);
        descendantRows.forEach(row -> allNodeIds.add(((Number) row.get("id")).longValue()));

        Map<Long, List<TraceabilityTreeResponse.LinkedIssue>> linkedIssuesByReq = new HashMap<>();
        for (Map<String, Object> row : traceabilityTreeRepository.findLinkedIssues(allNodeIds)) {
            Long requirementId = ((Number) row.get("requirement_id")).longValue();
            TraceabilityTreeResponse.LinkedIssue issue = TraceabilityTreeResponse.LinkedIssue.builder()
                    .id(((Number) row.get("issue_id")).longValue())
                    .issueKey((String) row.get("issue_key"))
                    .title((String) row.get("issue_title"))
                    .linkType(com.lightalm.domain.LinkType.valueOf((String) row.get("link_type")))
                    .status((String) row.get("issue_status"))
                    .build();
            linkedIssuesByReq.computeIfAbsent(requirementId, k -> new ArrayList<>()).add(issue);
        }

        Map<Long, List<Map<String, Object>>> childrenByParent = new HashMap<>();
        for (Map<String, Object> row : descendantRows) {
            Long parentId = ((Number) row.get("parent_requirement_id")).longValue();
            childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(row);
        }

        List<TraceabilityTreeResponse.DescendantNode> descendantTree = buildDescendantTree(reqId, childrenByParent, linkedIssuesByReq);

        TraceabilityTreeResponse.SelfNode selfNode = TraceabilityTreeResponse.SelfNode.builder()
                .id(self.getId())
                .reqKey(self.getReqKey())
                .title(self.getTitle())
                .status(self.getStatus().name())
                .build();

        return TraceabilityTreeResponse.builder()
                .ancestors(ancestors)
                .self(selfNode)
                .descendants(descendantTree)
                .build();
    }

    private List<TraceabilityTreeResponse.DescendantNode> buildDescendantTree(
            Long parentId, Map<Long, List<Map<String, Object>>> childrenByParent,
            Map<Long, List<TraceabilityTreeResponse.LinkedIssue>> linkedIssuesByReq) {
        List<Map<String, Object>> children = childrenByParent.getOrDefault(parentId, List.of());
        List<TraceabilityTreeResponse.DescendantNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : children) {
            Long id = ((Number) row.get("id")).longValue();
            nodes.add(TraceabilityTreeResponse.DescendantNode.builder()
                    .id(id)
                    .reqKey((String) row.get("req_key"))
                    .title((String) row.get("title"))
                    .status((String) row.get("status"))
                    .linkedIssues(linkedIssuesByReq.getOrDefault(id, List.of()))
                    .children(buildDescendantTree(id, childrenByParent, linkedIssuesByReq))
                    .build());
        }
        return nodes;
    }

    private boolean isRequirementIssuePair(TargetType a, TargetType b) {
        return (a == TargetType.REQUIREMENT && b == TargetType.ISSUE) || (a == TargetType.ISSUE && b == TargetType.REQUIREMENT);
    }

    private void validateTarget(Long projectId, TargetType type, Long id) {
        if (type == TargetType.REQUIREMENT) {
            Requirement requirement = requirementRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + id));
            if (!requirement.getProject().getId().equals(projectId)) {
                throw new ValidationException("요구사항이 해당 프로젝트에 속하지 않습니다: " + id);
            }
        } else {
            Issue issue = issueRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + id));
            if (!issue.getProject().getId().equals(projectId)) {
                throw new ValidationException("이슈가 해당 프로젝트에 속하지 않습니다: " + id);
            }
        }
    }
}
