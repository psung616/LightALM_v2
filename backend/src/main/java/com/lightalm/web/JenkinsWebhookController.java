package com.lightalm.web;

import com.lightalm.dto.JenkinsWebhookPayload;
import com.lightalm.service.JenkinsBuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/jenkins")
@RequiredArgsConstructor
public class JenkinsWebhookController {

    private final JenkinsBuildService jenkinsBuildService;

    @PostMapping("/{projectId}")
    public ResponseEntity<Void> handle(@PathVariable Long projectId,
                                        @RequestHeader(value = "X-Jenkins-Token", required = false) String token,
                                        @Valid @RequestBody JenkinsWebhookPayload payload) {
        if (!jenkinsBuildService.verifyWebhookToken(projectId, token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        jenkinsBuildService.handleWebhook(projectId, payload);
        return ResponseEntity.ok().build();
    }
}
