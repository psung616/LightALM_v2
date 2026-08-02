package com.lightalm.dto;

import com.lightalm.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    @NotNull(message = "role은 필수입니다.")
    private ProjectRole role;
}
