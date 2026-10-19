package com.instagramclone.admin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportStatus {
    OPEN,
    REVIEWED,
    RESOLVED,
    DISMISSED;

    public boolean isOpen() {
        return this == OPEN;
    }

    @JsonCreator
    public static ReportStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ReportStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReportStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
