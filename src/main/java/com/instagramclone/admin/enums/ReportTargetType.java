package com.instagramclone.admin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportTargetType {
    POST,
    USER,
    COMMENT,
    REEL,
    STORY,
    MESSAGE;

    @JsonCreator
    public static ReportTargetType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ReportTargetType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ReportTargetType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
