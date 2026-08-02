package com.lightalm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubIntegrationRequest {

    @NotBlank(message = "repoOwner는 필수입니다.")
    private String repoOwner;

    @NotBlank(message = "repoName은 필수입니다.")
    private String repoName;

    private String accessToken;

    private String webhookSecret;
}
