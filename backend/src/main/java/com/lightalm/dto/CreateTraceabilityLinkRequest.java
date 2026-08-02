package com.lightalm.dto;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTraceabilityLinkRequest {

    @NotNull(message = "sourceType은 필수입니다.")
    private TargetType sourceType;

    @NotNull(message = "sourceId는 필수입니다.")
    private Long sourceId;

    @NotNull(message = "targetType은 필수입니다.")
    private TargetType targetType;

    @NotNull(message = "targetId는 필수입니다.")
    private Long targetId;

    @NotNull(message = "linkType은 필수입니다.")
    private LinkType linkType;
}
