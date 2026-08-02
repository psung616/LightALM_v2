package com.lightalm.dto;

import com.lightalm.domain.SystemRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "username은 필수입니다.")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank(message = "email은 필수입니다.")
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank(message = "fullName은 필수입니다.")
    @Size(max = 100)
    private String fullName;

    private SystemRole systemRole;
}
