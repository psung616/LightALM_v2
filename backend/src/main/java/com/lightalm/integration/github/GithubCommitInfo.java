package com.lightalm.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubCommitInfo(
        String sha,
        Commit commit,
        Author author,
        @JsonProperty("html_url") String htmlUrl
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String login) {
    }

    public String message() {
        return commit != null ? commit.message() : null;
    }

    public String authorLogin() {
        return author != null ? author.login() : null;
    }
}
