package com.lightalm.license.service;

import com.lightalm.license.domain.LicenseDescriptor;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ADR-011 §2.2/§2.3 4단계: canonical 문자열에 대해 HMAC-SHA256을 재계산해 업로드된
 * {@code signature}와 상수 시간으로 비교한다. 대칭키(LICENSE_SIGNING_SECRET) 방식이며,
 * 강한 DRM이 아니라 "관리 UI를 거치지 않은 캐주얼 변조 방지" 수준의 보증이라는 점은
 * ADR-011 "결과" 섹션에 리스크로 기록되어 있다.
 */
@Component
public class LicenseSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String signingSecret;

    public LicenseSignatureVerifier(@Value("${light-alm.license.signing-secret}") String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public boolean isValid(LicenseDescriptor descriptor) {
        String expectedSignature = sign(descriptor.toCanonicalString());
        return constantTimeEquals(expectedSignature, descriptor.signature());
    }

    private String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 서명 계산에 실패했습니다.", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
