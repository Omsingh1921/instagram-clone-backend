package com.instagramclone.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    DEACTIVATED,
    SUSPENDED,
    DELETED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isBlockedOrSuspended() {
        return this == SUSPENDED || this == DELETED || this == DEACTIVATED;
    }

    @JsonCreator
    public static UserStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
