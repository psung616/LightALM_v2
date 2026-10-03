package com.lightalm.license.api;

import com.lightalm.dto.PageResponse;
import com.lightalm.license.dto.LicenseDetailResponse;
import com.lightalm.license.dto.LicenseSummaryResponse;
import com.lightalm.license.dto.LicenseUploadResponse;
import com.lightalm.license.service.LicenseCommandService;
import com.lightalm.license.service.LicenseQueryService;
import com.lightalm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * ADR-011 §2.4. 라이센스 파일 업로드/조회/이력 — ADMIN 전용.
 */
@RestController
@RequestMapping("/api/admin/licenses")
@RequiredArgsConstructor
public class LicenseAdminController {

    private final LicenseCommandService licenseCommandService;
    private final LicenseQueryService licenseQueryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LicenseUploadResponse upload(@RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return licenseCommandService.upload(file, principal);
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public LicenseDetailResponse current() {
        return licenseQueryService.getCurrent();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<LicenseSummaryResponse> history(
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return licenseQueryService.listHistory(pageable);
    }
}
