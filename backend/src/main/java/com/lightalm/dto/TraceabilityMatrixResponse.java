package com.lightalm.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TraceabilityMatrixResponse {
    private List<RequirementBrief> requirements;
    private List<IssueBrief> issues;
    private List<TraceabilityLinkResponse> links;

    @Getter
    @Builder
    public static class RequirementBrief {
        private Long id;
        private String reqKey;
        private String title;
    }

    @Getter
    @Builder
    public static class IssueBrief {
        private Long id;
        private String issueKey;
        private String title;
    }
}
