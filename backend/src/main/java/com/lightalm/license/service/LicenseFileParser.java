package com.lightalm.license.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightalm.license.domain.LicenseDescriptor;
import com.lightalm.license.domain.LicenseFileRejectedException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ADR-011 §2.3 3단계: 업로드된 raw 텍스트를 "Light ALM 라이센스 디스크립터" JSON으로 파싱한다.
 */
@Component
@RequiredArgsConstructor
public class LicenseFileParser {

    private static final int MAX_FILE_SIZE_BYTES = 16 * 1024;

    private final ObjectMapper objectMapper;

    public LicenseDescriptor parse(byte[] content) {
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new LicenseFileRejectedException("LICENSE_FILE_TOO_LARGE", "라이센스 파일은 16KB를 초과할 수 없습니다.");
        }
        try {
            return objectMapper.readValue(content, LicenseDescriptor.class);
        } catch (IOException e) {
            throw new LicenseFileRejectedException("LICENSE_FILE_INVALID", "라이센스 파일을 파싱할 수 없습니다: " + e.getMessage());
        }
    }
}
