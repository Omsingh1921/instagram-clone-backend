package com.instagramclone.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_MODERATOR;

    public boolean isAdmin() {
        return this == ROLE_ADMIN;
    }

    public boolean isModerator() {
        return this == ROLE_MODERATOR;
    }

    public boolean isStaff() {
        return this == ROLE_ADMIN || this == ROLE_MODERATOR;
    }

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(value.trim()) ||
                role.name().replace("ROLE_", "").equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown Role: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
