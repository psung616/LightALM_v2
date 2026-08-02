package com.lightalm.dto;

import com.lightalm.domain.LinkType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceabilityTreeResponse {
    private List<AncestorNode> ancestors;
    private SelfNode self;
    private List<DescendantNode> descendants;

    @Getter
    @Builder
    public static class AncestorNode {
        private Long id;
        private String reqKey;
        private String title;
    }

    @Getter
    @Builder
    public static class SelfNode {
        private Long id;
        private String reqKey;
        private String title;
        private String status;
    }

    @Getter
    @Builder
    public static class DescendantNode {
        private Long id;
        private String reqKey;
        private String title;
        private String status;
        private List<LinkedIssue> linkedIssues;
        private List<DescendantNode> children;
    }

    @Getter
    @Builder
    public static class LinkedIssue {
        private Long id;
        private String issueKey;
        private String title;
        private LinkType linkType;
        private String status;
    }
}
