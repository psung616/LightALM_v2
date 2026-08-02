package com.lightalm.dto;

import com.lightalm.domain.SystemRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "email은 필수입니다.")
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank(message = "fullName은 필수입니다.")
    @Size(max = 100)
    private String fullName;

    private SystemRole systemRole;

    private Boolean enabled;
}
