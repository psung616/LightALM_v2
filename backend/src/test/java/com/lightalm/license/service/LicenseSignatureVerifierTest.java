package com.lightalm.license.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.license.domain.LicenseDescriptor;
import com.lightalm.license.domain.LicenseType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * ADR-011 §2.2/§2.3: 서명 위조 파일 거부 여부에 대한 회귀 테스트.
 */
class LicenseSignatureVerifierTest {

    private static final String SECRET = "test-signing-secret";

    private final LicenseSignatureVerifier verifier = new LicenseSignatureVerifier(SECRET);

    @Test
    void isValid_returnsTrue_whenSignatureMatchesCanonicalString() {
        LicenseDescriptor descriptor = descriptorWithValidSignature(50, "2027-01-01T00:00:00Z");

        assertThat(verifier.isValid(descriptor)).isTrue();
    }

    @Test
    void isValid_returnsFalse_whenSignatureIsForged() {
        LicenseDescriptor valid = descriptorWithValidSignature(50, "2027-01-01T00:00:00Z");
        LicenseDescriptor forged = new LicenseDescriptor(
                valid.licenseKey(), valid.organizationName(), valid.licenseType(), valid.seatLimit(),
                valid.issuedAt(), valid.expiresAt(), "forged-signature-value");

        assertThat(verifier.isValid(forged)).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenPayloadWasTamperedAfterSigning() {
        LicenseDescriptor valid = descriptorWithValidSignature(50, "2027-01-01T00:00:00Z");
        // seatLimit을 100으로 몰래 올렸지만 signature는 원래 50에 대해 서명된 값
        LicenseDescriptor tampered = new LicenseDescriptor(
                valid.licenseKey(), valid.organizationName(), valid.licenseType(), 100,
                valid.issuedAt(), valid.expiresAt(), valid.signature());

        assertThat(verifier.isValid(tampered)).isFalse();
    }

    private LicenseDescriptor descriptorWithValidSignature(int seatLimit, String expiresAt) {
        LicenseDescriptor unsigned = new LicenseDescriptor(
                "LALM-LIC-2026-0001", "ACME Corp", LicenseType.STANDARD, seatLimit,
                "2026-01-01T00:00:00Z", expiresAt, null);
        String signature = sign(unsigned.toCanonicalString());
        return new LicenseDescriptor(
                unsigned.licenseKey(), unsigned.organizationName(), unsigned.licenseType(), unsigned.seatLimit(),
                unsigned.issuedAt(), unsigned.expiresAt(), signature);
    }

    private String sign(String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
