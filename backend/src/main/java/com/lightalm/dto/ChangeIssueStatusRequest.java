package com.lightalm.dto;

import com.lightalm.domain.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeIssueStatusRequest {

    @NotNull(message = "status는 필수입니다.")
    private IssueStatus status;
}
