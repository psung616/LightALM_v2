package com.lightalm.license.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseInvalidException;
import com.lightalm.license.domain.LicenseSeatLimitExceededException;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.domain.LicenseType;
import com.lightalm.license.repository.LicenseRepository;
import com.lightalm.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-011 §2.3 게이트 A/B 회귀 테스트.
 */
@ExtendWith(MockitoExtension.class)
class LicenseEnforcementServiceTest {

    @Mock
    private LicenseRepository licenseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LicenseEnforcementService licenseEnforcementService;

    private License activeLicense(int seatLimit, LocalDateTime expiresAt) {
        return License.builder()
                .licenseKey("LALM-LIC-TEST")
                .organizationName("ACME Corp")
                .licenseType(LicenseType.STANDARD)
                .seatLimit(seatLimit)
                .issuedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(expiresAt)
                .rawFileName("license.json")
                .rawPayload("{}")
                .signatureValid(true)
                .build();
    }

    @Test
    void requireActiveLicense_throwsLicenseInvalid_whenNoActiveLicenseExists() {
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licenseEnforcementService.requireActiveLicense())
                .isInstanceOf(LicenseInvalidException.class);
    }

    @Test
    void requireActiveLicense_throwsLicenseInvalid_whenActiveLicenseIsExpired() {
        License expired = activeLicense(50, LocalDateTime.now().minusDays(1));
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> licenseEnforcementService.requireActiveLicense())
                .isInstanceOf(LicenseInvalidException.class);
    }

    @Test
    void requireActiveLicense_passes_whenActiveLicenseNotExpired() {
        License valid = activeLicense(50, LocalDateTime.now().plusDays(30));
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(valid));

        assertThatCode(() -> licenseEnforcementService.requireActiveLicense()).doesNotThrowAnyException();
    }

    @Test
    void requireActiveLicense_passes_whenLicenseIsUnlimited() {
        License unlimited = activeLicense(10, null);
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(unlimited));

        assertThatCode(() -> licenseEnforcementService.requireActiveLicense()).doesNotThrowAnyException();
    }

    @Test
    void requireSeatAvailable_throwsSeatLimitExceeded_whenEnabledUserCountReachesSeatLimit() {
        License license = activeLicense(10, LocalDateTime.now().plusDays(30));
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(license));
        when(userRepository.countByEnabledTrue()).thenReturn(10L);

        assertThatThrownBy(() -> licenseEnforcementService.requireSeatAvailable())
                .isInstanceOf(LicenseSeatLimitExceededException.class);
    }

    @Test
    void requireSeatAvailable_passes_whenSeatsRemain() {
        License license = activeLicense(10, LocalDateTime.now().plusDays(30));
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.of(license));
        when(userRepository.countByEnabledTrue()).thenReturn(9L);

        assertThatCode(() -> licenseEnforcementService.requireSeatAvailable()).doesNotThrowAnyException();
    }

    @Test
    void requireSeatAvailable_throwsLicenseInvalid_whenNoActiveLicenseExists() {
        when(licenseRepository.findByStatus(LicenseStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> licenseEnforcementService.requireSeatAvailable())
                .isInstanceOf(LicenseInvalidException.class);
    }
}
