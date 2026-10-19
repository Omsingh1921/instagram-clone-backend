package com.instagramclone.admin.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ModerationAction {
    NO_ACTION,
    REMOVE_CONTENT,
    SUSPEND_USER,
    WARN_USER;

    @JsonCreator
    public static ModerationAction fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NO_ACTION;
        }
        for (ModerationAction action : values()) {
            if (action.name().equalsIgnoreCase(value.trim())) {
                return action;
            }
        }
        return NO_ACTION;
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
