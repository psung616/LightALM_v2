package com.lightalm.dto;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TraceabilityLink;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceabilityLinkResponse {
    private Long id;
    private Long requirementId;
    private Long issueId;
    private Long testCaseId;
    private LinkType linkType;

    /**
     * ADR-013 버그 수정(qa-tester 발견): source/target 중 REQUIREMENT가 아니면 무조건
     * ISSUE로 간주하던 기존 로직이 TEST_CASE가 끼어 있는 링크에서 requirementId/issueId에
     * 테스트케이스 id를 잘못 채워 넣는 문제가 있었다. 세 타입(REQUIREMENT/ISSUE/TEST_CASE)을
     * 각각 source/target 어느 쪽에 있든 정확히 찾아 채우고, 해당 타입이 링크에 없으면 null로 둔다.
     */
    public static TraceabilityLinkResponse from(TraceabilityLink link) {
        return TraceabilityLinkResponse.builder()
                .id(link.getId())
                .requirementId(resolveIdForType(link, TargetType.REQUIREMENT))
                .issueId(resolveIdForType(link, TargetType.ISSUE))
                .testCaseId(resolveIdForType(link, TargetType.TEST_CASE))
                .linkType(link.getLinkType())
                .build();
    }

    private static Long resolveIdForType(TraceabilityLink link, TargetType type) {
        if (link.getSourceType() == type) {
            return link.getSourceId();
        }
        if (link.getTargetType() == type) {
            return link.getTargetId();
        }
        return null;
    }
}
