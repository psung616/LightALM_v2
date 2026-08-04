package com.lightalm.dto;

import com.lightalm.domain.RequirementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateApprovalRequestRequest {

    @NotNull(message = "requestedStatus는 필수입니다.")
    private RequirementStatus requestedStatus;
}
