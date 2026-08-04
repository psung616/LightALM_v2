package com.lightalm.dto;

import com.lightalm.domain.TestResult;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordTestResultRequest {

    @NotNull(message = "result는 필수입니다.")
    private TestResult result;

    private String actualResult;
}
