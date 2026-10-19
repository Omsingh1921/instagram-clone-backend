package com.instagramclone.chat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConversationType {
    DIRECT,
    GROUP;

    public boolean isDirect() {
        return this == DIRECT;
    }

    public boolean isGroup() {
        return this == GROUP;
    }

    @JsonCreator
    public static ConversationType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ConversationType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ConversationType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
