package com.instagramclone.reel.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReelStatus {
    READY,
    PROCESSING,
    FAILED;

    public boolean isReady() {
        return this == READY;
    }

    public boolean isProcessing() {
        return this == PROCESSING;
    }

    @JsonCreator
    public static ReelStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ReelStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReelStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
