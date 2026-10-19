package com.instagramclone.chat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConversationRole {
    ADMIN,
    MEMBER;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    @JsonCreator
    public static ConversationRole fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ConversationRole role : values()) {
            if (role.name().equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown ConversationRole: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
