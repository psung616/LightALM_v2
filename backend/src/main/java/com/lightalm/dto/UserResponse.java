package com.lightalm.dto;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.security.UserPrincipal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private SystemRole systemRole;
    private boolean enabled;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .systemRole(user.getSystemRole())
                .enabled(user.getEnabled())
                .build();
    }

    public static UserResponse from(UserPrincipal principal) {
        return UserResponse.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .systemRole(principal.getSystemRole())
                .enabled(principal.isEnabled())
                .build();
    }
}
