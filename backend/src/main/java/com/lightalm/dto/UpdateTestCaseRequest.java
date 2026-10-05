package com.lightalm.dto;

import com.lightalm.domain.TestCaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTestCaseRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 255)
    private String title;

    private String description;

    private String preconditions;

    @NotBlank(message = "steps는 필수입니다.")
    private String steps;

    @NotBlank(message = "expectedResult는 필수입니다.")
    private String expectedResult;

    /** ADR-012 §C.3: 프로젝트가 PRIORITY 집합을 확장했으면 그 값도 허용된다(검증은 서비스 레이어). */
    private String priority;

    private Long requirementId;

    private TestCaseStatus status;
}
