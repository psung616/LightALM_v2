package com.lightalm.dto;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.TraceabilityLink;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceabilityLinkResponse {
    private Long id;
    private Long requirementId;
    private Long issueId;
    private LinkType linkType;

    public static TraceabilityLinkResponse from(TraceabilityLink link) {
        Long requirementId = com.lightalm.domain.TargetType.REQUIREMENT.equals(link.getSourceType()) ? link.getSourceId() : link.getTargetId();
        Long issueId = com.lightalm.domain.TargetType.ISSUE.equals(link.getSourceType()) ? link.getSourceId() : link.getTargetId();
        return TraceabilityLinkResponse.builder()
                .id(link.getId())
                .requirementId(requirementId)
                .issueId(issueId)
                .linkType(link.getLinkType())
                .build();
    }
}
