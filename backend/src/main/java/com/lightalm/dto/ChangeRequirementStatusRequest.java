package com.lightalm.dto;

import com.lightalm.domain.RequirementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRequirementStatusRequest {

    @NotNull(message = "status는 필수입니다.")
    private RequirementStatus status;
}
