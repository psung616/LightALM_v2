package com.lightalm.integration.github;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * GitHub Webhook 서명 검증(§4.9): X-Hub-Signature-256 헤더는 "sha256=" + HMAC-SHA256(payload, secret)(hex) 형태다.
 */
@Component
public class GithubWebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    public boolean isValid(byte[] payload, String signatureHeader, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX) || secret == null || secret.isBlank()) {
            return false;
        }
        String expectedHex = computeHmacSha256Hex(payload, secret);
        String providedHex = signatureHeader.substring(PREFIX.length());
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                providedHex.getBytes(StandardCharsets.UTF_8));
    }

    private String computeHmacSha256Hex(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(payload);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 서명 계산에 실패했습니다.", e);
        }
    }
}
