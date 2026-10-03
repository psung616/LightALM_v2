package com.lightalm.license.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.license.domain.LicenseDescriptor;
import com.lightalm.license.domain.LicenseFileRejectedException;
import com.lightalm.license.domain.LicenseType;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class LicenseFileParserTest {

    private final LicenseFileParser parser = new LicenseFileParser(new ObjectMapper());

    @Test
    void parse_returnsDescriptor_whenJsonIsWellFormed() {
        String json = """
                {
                  "licenseKey": "LALM-LIC-2026-0001",
                  "organizationName": "ACME Corp",
                  "licenseType": "STANDARD",
                  "seatLimit": 50,
                  "issuedAt": "2026-01-01T00:00:00Z",
                  "expiresAt": "2027-01-01T00:00:00Z",
                  "signature": "dummy-signature"
                }
                """;

        LicenseDescriptor descriptor = parser.parse(json.getBytes(StandardCharsets.UTF_8));

        assertThat(descriptor.licenseKey()).isEqualTo("LALM-LIC-2026-0001");
        assertThat(descriptor.organizationName()).isEqualTo("ACME Corp");
        assertThat(descriptor.licenseType()).isEqualTo(LicenseType.STANDARD);
        assertThat(descriptor.seatLimit()).isEqualTo(50);
        assertThat(descriptor.signature()).isEqualTo("dummy-signature");
    }

    @Test
    void parse_throwsLicenseFileTooLarge_whenContentExceeds16KB() {
        byte[] oversized = new byte[16 * 1024 + 1];

        ThrowingCallable call = () -> parser.parse(oversized);

        assertThatThrownBy(call)
                .isInstanceOf(LicenseFileRejectedException.class)
                .satisfies(ex -> assertThat(((LicenseFileRejectedException) ex).getErrorCode()).isEqualTo("LICENSE_FILE_TOO_LARGE"));
    }

    @Test
    void parse_throwsLicenseFileInvalid_whenJsonIsMalformed() {
        byte[] malformed = "{ not valid json".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(malformed))
                .isInstanceOf(LicenseFileRejectedException.class);
    }
}
