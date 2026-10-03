package com.lightalm.license.service;

import com.lightalm.dto.PageResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.license.domain.License;
import com.lightalm.license.domain.LicenseStatus;
import com.lightalm.license.dto.LicenseDetailResponse;
import com.lightalm.license.dto.LicenseSummaryResponse;
import com.lightalm.license.dto.PublicLicenseStatusResponse;
import com.lightalm.license.repository.LicenseRepository;
import com.lightalm.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LicenseQueryService {

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;

    public LicenseDetailResponse getCurrent() {
        License active = licenseRepository.findByStatus(LicenseStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("활성화된 라이센스가 없습니다."));
        long seatsUsed = userRepository.countByEnabledTrue();
        return LicenseDetailResponse.from(active, seatsUsed);
    }

    public PageResponse<LicenseSummaryResponse> listHistory(Pageable pageable) {
        return PageResponse.from(licenseRepository.findAllByOrderByUploadedAtDesc(pageable).map(LicenseSummaryResponse::from));
    }

    public PublicLicenseStatusResponse getPublicStatus() {
        Optional<License> active = licenseRepository.findByStatus(LicenseStatus.ACTIVE);
        if (active.isEmpty()) {
            return new PublicLicenseStatusResponse(false, "활성화된 라이센스가 없습니다.");
        }
        License license = active.get();
        if (license.isExpired(LocalDateTime.now())) {
            return new PublicLicenseStatusResponse(false, "라이센스가 만료되었습니다.");
        }
        long seatsUsed = userRepository.countByEnabledTrue();
        if (seatsUsed >= license.getSeatLimit()) {
            return new PublicLicenseStatusResponse(false, "시트 한도에 도달했습니다.");
        }
        return new PublicLicenseStatusResponse(true, null);
    }
}
