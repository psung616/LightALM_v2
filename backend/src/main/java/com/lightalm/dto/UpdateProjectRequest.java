package com.lightalm.dto;

import com.lightalm.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

    @NotBlank(message = "name은 필수입니다.")
    @Size(max = 150)
    private String name;

    private String description;

    private ProjectStatus status;
}
