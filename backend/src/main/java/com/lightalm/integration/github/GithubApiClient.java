package com.lightalm.integration.github;

import com.lightalm.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GithubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GithubApiClient.class);

    private final RestClient restClient;

    public GithubApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public GithubCommitInfo getCommit(String owner, String repo, String sha, String accessToken) {
        try {
            return requestSpec(owner, repo, "/commits/" + sha, accessToken)
                    .retrieve()
                    .body(GithubCommitInfo.class);
        } catch (RestClientException e) {
            log.warn("GitHub commit 조회 실패: {}/{}@{}", owner, repo, sha, e);
            throw new ExternalApiException("GITHUB_API_ERROR", "GitHub 커밋 정보를 조회하지 못했습니다: " + sha);
        }
    }

    public GithubPullRequestInfo getPullRequest(String owner, String repo, int number, String accessToken) {
        try {
            return requestSpec(owner, repo, "/pulls/" + number, accessToken)
                    .retrieve()
                    .body(GithubPullRequestInfo.class);
        } catch (RestClientException e) {
            log.warn("GitHub PR 조회 실패: {}/{}#{}", owner, repo, number, e);
            throw new ExternalApiException("GITHUB_API_ERROR", "GitHub PR 정보를 조회하지 못했습니다: #" + number);
        }
    }

    private RestClient.RequestHeadersSpec<?> requestSpec(String owner, String repo, String path, String accessToken) {
        RestClient.RequestHeadersSpec<?> spec = restClient.get().uri("/repos/{owner}/{repo}{path}", owner, repo, path);
        if (accessToken != null && !accessToken.isBlank()) {
            spec.header("Authorization", "Bearer " + accessToken);
        }
        return spec;
    }
}
