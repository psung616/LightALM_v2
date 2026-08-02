package com.lightalm.web;

import com.lightalm.service.GitLinkService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/github")
@RequiredArgsConstructor
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);

    private final GitLinkService gitLinkService;

    @PostMapping("/{projectId}")
    public ResponseEntity<Void> handle(@PathVariable Long projectId,
                                        @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
                                        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
                                        @RequestBody byte[] rawBody) {
        if (!gitLinkService.verifyWebhookSignature(projectId, rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if ("push".equals(eventType)) {
            gitLinkService.handlePushEvent(projectId, rawBody);
        } else if ("pull_request".equals(eventType)) {
            gitLinkService.handlePullRequestEvent(projectId, rawBody);
        } else {
            log.info("처리하지 않는 GitHub Webhook 이벤트 유형: {}", eventType);
        }
        return ResponseEntity.ok().build();
    }
}
