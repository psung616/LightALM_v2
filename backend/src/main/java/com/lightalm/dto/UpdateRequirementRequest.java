package com.lightalm.dto;

import com.lightalm.domain.Priority;
import com.lightalm.domain.RequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequirementRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "type은 필수입니다.")
    private RequirementType type;

    private Priority priority;

    private Long parentRequirementId;

    private Long assignedTo;

    private LocalDate dueDate;
}
