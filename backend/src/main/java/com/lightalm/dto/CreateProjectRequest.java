package com.lightalm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "projectKey는 필수입니다.")
    @Pattern(regexp = "^[A-Z]{3,10}$", message = "projectKey는 대문자 3~10자여야 합니다.")
    private String projectKey;

    @NotBlank(message = "name은 필수입니다.")
    @Size(max = 150)
    private String name;

    private String description;
}
