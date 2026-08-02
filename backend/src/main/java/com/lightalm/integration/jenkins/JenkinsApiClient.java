package com.lightalm.integration.jenkins;

import com.lightalm.exception.ExternalApiException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JenkinsApiClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsApiClient.class);

    private final RestClient restClient = RestClient.create();

    public void triggerBuild(String baseUrl, String jobName, String apiUser, String apiToken) {
        String url = normalizeBaseUrl(baseUrl) + "/job/" + jobName + "/build";
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.post().uri(url);
            if (apiUser != null && !apiUser.isBlank()) {
                String credentials = Base64.getEncoder().encodeToString(
                        (apiUser + ":" + (apiToken != null ? apiToken : "")).getBytes(StandardCharsets.UTF_8));
                request.header("Authorization", "Basic " + credentials);
            }
            request.retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Jenkins 빌드 트리거 실패: {}", url, e);
            throw new ExternalApiException("JENKINS_API_ERROR", "Jenkins 빌드 트리거에 실패했습니다: " + jobName);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
