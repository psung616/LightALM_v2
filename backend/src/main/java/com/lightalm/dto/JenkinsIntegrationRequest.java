package com.lightalm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JenkinsIntegrationRequest {

    @NotBlank(message = "baseUrl은 필수입니다.")
    private String baseUrl;

    @NotBlank(message = "jobName은 필수입니다.")
    private String jobName;

    private String apiUser;

    private String apiToken;
}
