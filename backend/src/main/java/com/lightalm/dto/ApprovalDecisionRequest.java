package com.lightalm.dto;

import com.lightalm.domain.ApprovalDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalDecisionRequest {

    @NotNull(message = "decision은 필수입니다.")
    private ApprovalDecision decision;

    private String comment;
}
