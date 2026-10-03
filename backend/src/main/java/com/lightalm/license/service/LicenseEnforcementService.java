package com.lightalm.license.service;

import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseInvalidException;
import com.lightalm.license.domain.LicenseSeatLimitExceededException;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.repository.LicenseRepository;
import com.lightalm.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-011 §2.3 게이트 A/B. 이름의 동사 규칙(require~)은 {@code ProjectMemberService.requireRole(...)}과
 * 동일한 패턴을 따른다(GLOSSARY.md §5).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LicenseEnforcementService {

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;

    /**
     * 게이트 A: 활성 라이센스가 없거나 만료되었으면 LicenseInvalidException(403 LICENSE_INVALID).
     */
    public void requireActiveLicense() {
        License active = findActiveLicense();
        if (active.isExpired(LocalDateTime.now())) {
            throw new LicenseInvalidException("라이센스가 만료되었습니다.");
        }
    }

    /**
     * 게이트 B: enabled=true 사용자 수가 seat_limit에 도달했으면 LicenseSeatLimitExceededException(403).
     * 호출 전에 requireActiveLicense()가 이미 통과했다는 전제를 두지 않고, 활성 라이센스가 없으면
     * 동일하게 LicenseInvalidException을 던진다.
     */
    public void requireSeatAvailable() {
        License active = findActiveLicense();
        long seatsUsed = userRepository.countByEnabledTrue();
        if (seatsUsed >= active.getSeatLimit()) {
            throw new LicenseSeatLimitExceededException("시트 한도를 초과했습니다.");
        }
    }

    private License findActiveLicense() {
        return licenseRepository.findByStatus(LicenseStatus.ACTIVE)
                .orElseThrow(() -> new LicenseInvalidException("활성화된 라이센스가 없습니다."));
    }
}
