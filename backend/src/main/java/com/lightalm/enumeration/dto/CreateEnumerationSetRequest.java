package com.lightalm.enumeration.dto;

import com.lightalm.enumeration.domain.BaseEnumType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ADR-012 §C.4 POST 요청 바디. baseEnum은 생략(커스텀 필드 전용) 또는 PRIORITY만 허용한다 —
 * REQUIREMENT_STATUS/ISSUE_STATUS/TEST_CASE_STATUS는 서비스 레이어가 거부한다(§C.1).
 */
public record CreateEnumerationSetRequest(
        @NotBlank(message = "enumKey는 필수입니다.")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,49}$", message = "enumKey는 대문자로 시작하는 대문자/숫자/언더바 조합이어야 합니다.")
        String enumKey,
        BaseEnumType baseEnum,
        @NotBlank(message = "name은 필수입니다.") @Size(max = 100) String name
) {
}
