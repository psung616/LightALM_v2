package com.lightalm.dto;

import com.lightalm.domain.ReleaseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeReleaseStatusRequest {

    @NotNull(message = "status는 필수입니다.")
    private ReleaseStatus status;
}
