package com.lightalm.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTestCasesToRunRequest {

    @NotEmpty(message = "testCaseIds는 최소 1개 이상이어야 합니다.")
    private List<Long> testCaseIds;
}
