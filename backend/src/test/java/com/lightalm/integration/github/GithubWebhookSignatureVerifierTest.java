package com.lightalm.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class GithubWebhookSignatureVerifierTest {

    private final GithubWebhookSignatureVerifier verifier = new GithubWebhookSignatureVerifier();

    @Test
    void isValid_acceptsCorrectSignature() throws Exception {
        byte[] payload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "mysecret123";
        String signature = "sha256=" + hmacSha256Hex(payload, secret);

        assertThat(verifier.isValid(payload, signature, secret)).isTrue();
    }

    @Test
    void isValid_rejectsWrongSecret() throws Exception {
        byte[] payload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + hmacSha256Hex(payload, "mysecret123");

        assertThat(verifier.isValid(payload, signature, "wrong-secret")).isFalse();
    }

    @Test
    void isValid_rejectsTamperedPayload() throws Exception {
        byte[] original = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "mysecret123";
        String signature = "sha256=" + hmacSha256Hex(original, secret);

        byte[] tampered = "{\"ref\":\"refs/heads/evil\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.isValid(tampered, signature, secret)).isFalse();
    }

    @Test
    void isValid_rejectsMissingPrefix() {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(payload, "deadbeef", "secret")).isFalse();
    }

    @Test
    void isValid_rejectsNullSignatureOrSecret() {
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isValid(payload, null, "secret")).isFalse();
        assertThat(verifier.isValid(payload, "sha256=abc", null)).isFalse();
    }

    private String hmacSha256Hex(byte[] payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
