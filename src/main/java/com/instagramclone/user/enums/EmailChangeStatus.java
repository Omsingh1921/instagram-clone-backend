package com.instagramclone.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status for two-step email change verification requests.
 */
public enum EmailChangeStatus {
    PENDING,
    COMPLETED,
    CANCELLED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    @JsonCreator
    public static EmailChangeStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (EmailChangeStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown EmailChangeStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
