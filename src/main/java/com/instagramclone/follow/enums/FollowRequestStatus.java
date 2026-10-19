package com.instagramclone.follow.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FollowRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    public boolean isPending() {
        return this == PENDING;
    }

    @JsonCreator
    public static FollowRequestStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (FollowRequestStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown FollowRequestStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
