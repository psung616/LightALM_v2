package com.lightalm.dto;

import com.lightalm.domain.GitLink;
import com.lightalm.domain.GitLinkSource;
import com.lightalm.domain.PrStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GitLinkResponse {
    private Long id;
    private GitLinkSource source;
    private String commitSha;
    private Integer prNumber;
    private PrStatus prStatus;
    private String message;
    private String authorLogin;
    private String url;
    private LocalDateTime linkedAt;

    public static GitLinkResponse from(GitLink link) {
        return GitLinkResponse.builder()
                .id(link.getId())
                .source(link.getSource())
                .commitSha(link.getCommitSha())
                .prNumber(link.getPrNumber())
                .prStatus(link.getPrStatus())
                .message(link.getMessage())
                .authorLogin(link.getAuthorLogin())
                .url(link.getUrl())
                .linkedAt(link.getLinkedAt())
                .build();
    }
}
