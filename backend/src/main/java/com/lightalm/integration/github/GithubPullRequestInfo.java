package com.lightalm.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubPullRequestInfo(
        Integer number,
        String title,
        String body,
        String state,
        Boolean merged,
        @JsonProperty("html_url") String htmlUrl,
        User user
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String login) {
    }

    public String authorLogin() {
        return user != null ? user.login() : null;
    }
}
