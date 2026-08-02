package com.lightalm.domain;

public enum ProjectRole {
    VIEWER,
    MEMBER,
    PROJECT_ADMIN;

    public boolean isAtLeast(ProjectRole minRole) {
        return this.ordinal() >= minRole.ordinal();
    }
}
