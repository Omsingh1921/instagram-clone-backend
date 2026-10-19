package com.instagramclone.post.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MediaType {
    IMAGE,
    VIDEO;

    public boolean isVideo() {
        return this == VIDEO;
    }

    public boolean isImage() {
        return this == IMAGE;
    }

    @JsonCreator
    public static MediaType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (MediaType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MediaType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
