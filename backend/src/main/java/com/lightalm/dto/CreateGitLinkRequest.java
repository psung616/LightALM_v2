package com.lightalm.dto;

import com.lightalm.domain.GitLinkSource;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGitLinkRequest {

    @NotNull(message = "source는 필수입니다.")
    private GitLinkSource source;

    private String commitSha;

    private Integer prNumber;
}
