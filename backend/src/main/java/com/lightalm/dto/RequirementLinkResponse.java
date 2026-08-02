package com.lightalm.dto;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.TargetType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequirementLinkResponse {
    private Long linkId;
    private TargetType linkedType;
    private Long linkedId;
    private String linkedKey;
    private String linkedTitle;
    private String linkedStatus;
    private LinkType linkType;
}
