package com.lightalm.dto;

import com.lightalm.domain.RequirementLevel;
import com.lightalm.domain.RequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRequirementRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "type은 필수입니다.")
    private RequirementType type;

    /** ADR-012 §C.3: 프로젝트가 PRIORITY 집합을 확장했으면 그 값도 허용된다(검증은 서비스 레이어). */
    private String priority;

    private Long parentRequirementId;

    private Long assignedTo;

    private LocalDate dueDate;

    /** 생략 시 서버가 SRS로 저장한다 (ADR-013). */
    private RequirementLevel requirementLevel;
}
