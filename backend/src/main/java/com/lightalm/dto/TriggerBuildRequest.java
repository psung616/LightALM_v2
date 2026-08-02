package com.lightalm.dto;

import com.lightalm.domain.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerBuildRequest {

    @NotNull(message = "targetType은 필수입니다.")
    private TargetType targetType;

    @NotNull(message = "targetId는 필수입니다.")
    private Long targetId;
}
