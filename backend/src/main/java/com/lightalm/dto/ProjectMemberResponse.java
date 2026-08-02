package com.lightalm.dto;

import com.lightalm.domain.ProjectMember;
import com.lightalm.domain.ProjectRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMemberResponse {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private ProjectRole role;
    private LocalDateTime joinedAt;

    public static ProjectMemberResponse from(ProjectMember member) {
        return ProjectMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .fullName(member.getUser().getFullName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
