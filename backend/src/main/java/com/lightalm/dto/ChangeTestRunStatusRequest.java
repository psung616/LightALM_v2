package com.lightalm.dto;

import com.lightalm.domain.TestRunStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTestRunStatusRequest {

    @NotNull(message = "status는 필수입니다.")
    private TestRunStatus status;
}
