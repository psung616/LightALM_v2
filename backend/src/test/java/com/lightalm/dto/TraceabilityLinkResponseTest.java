package com.lightalm.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TraceabilityLink;
import org.junit.jupiter.api.Test;

/**
 * ADR-013 버그 회귀 테스트(qa-tester 발견): source/target 중 REQUIREMENT가 아니면
 * 무조건 ISSUE로 간주하던 기존 {@code from()}이 TEST_CASE가 끼어 있는 링크에서
 * requirementId/issueId에 테스트케이스 id를 잘못 채워 넣는 문제가 있었다.
 */
class TraceabilityLinkResponseTest {

    @Test
    void from_whenSourceIsTestCaseAndTargetIsIssue_fillsTestCaseIdAndIssueIdOnly() {
        TraceabilityLink link = TraceabilityLink.builder()
                .id(1L)
                .sourceType(TargetType.TEST_CASE)
                .sourceId(5L)
                .targetType(TargetType.ISSUE)
                .targetId(7L)
                .linkType(LinkType.RELATES_TO)
                .build();

        TraceabilityLinkResponse response = TraceabilityLinkResponse.from(link);

        assertThat(response.getTestCaseId()).isEqualTo(5L);
        assertThat(response.getIssueId()).isEqualTo(7L);
        assertThat(response.getRequirementId()).isNull();
    }

    @Test
    void from_whenSourceIsRequirementAndTargetIsIssue_behavesAsBefore() {
        TraceabilityLink link = TraceabilityLink.builder()
                .id(2L)
                .sourceType(TargetType.REQUIREMENT)
                .sourceId(3L)
                .targetType(TargetType.ISSUE)
                .targetId(4L)
                .linkType(LinkType.IMPLEMENTS)
                .build();

        TraceabilityLinkResponse response = TraceabilityLinkResponse.from(link);

        assertThat(response.getRequirementId()).isEqualTo(3L);
        assertThat(response.getIssueId()).isEqualTo(4L);
        assertThat(response.getTestCaseId()).isNull();
    }

    @Test
    void from_whenSourceIsRequirementAndTargetIsTestCase_fillsRequirementIdAndTestCaseIdOnly() {
        TraceabilityLink link = TraceabilityLink.builder()
                .id(3L)
                .sourceType(TargetType.REQUIREMENT)
                .sourceId(1L)
                .targetType(TargetType.TEST_CASE)
                .targetId(9L)
                .linkType(LinkType.TESTS)
                .build();

        TraceabilityLinkResponse response = TraceabilityLinkResponse.from(link);

        assertThat(response.getRequirementId()).isEqualTo(1L);
        assertThat(response.getTestCaseId()).isEqualTo(9L);
        assertThat(response.getIssueId()).isNull();
    }
}
