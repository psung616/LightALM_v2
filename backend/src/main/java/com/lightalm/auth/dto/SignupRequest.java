package com.lightalm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ADR-011 §1.2. systemRole 필드를 두지 않아 권한 상승 경로를 원천 차단한다.
 */
public record SignupRequest(
        @NotBlank(message = "username은 필수입니다.") @Size(max = 50) String username,
        @NotBlank(message = "email은 필수입니다.") @Email @Size(max = 120) String email,
        @NotBlank(message = "fullName은 필수입니다.") @Size(max = 100) String fullName,
        @NotBlank(message = "password는 필수입니다.") @Size(min = 8, max = 100) String password,
        @NotBlank(message = "passwordConfirm은 필수입니다.") String passwordConfirm
) {
}
