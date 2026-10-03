package com.lightalm.license.api;

import com.lightalm.license.dto.PublicLicenseStatusResponse;
import com.lightalm.license.service.LicenseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-011 §2.4. 공개 엔드포인트 — /signup 화면이 폼 노출 전에 호출한다.
 */
@RestController
@RequestMapping("/api/public/license-status")
@RequiredArgsConstructor
public class PublicLicenseStatusController {

    private final LicenseQueryService licenseQueryService;

    @GetMapping
    public PublicLicenseStatusResponse status() {
        return licenseQueryService.getPublicStatus();
    }
}
