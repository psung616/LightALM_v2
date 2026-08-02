package com.lightalm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "oldPassword는 필수입니다.")
    private String oldPassword;

    @NotBlank(message = "newPassword는 필수입니다.")
    @Size(min = 8, max = 100)
    private String newPassword;
}
