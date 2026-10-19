package com.instagramclone.notification.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    FOLLOW,
    POST_LIKED,
    POST_COMMENTED,
    COMMENT_REPLIED,
    USER_MENTIONED,
    USER_TAGGED,
    SYSTEM,
    FOLLOW_REQUEST,
    FOLLOW_REQUEST_ACCEPTED,
    REEL_LIKED,
    REEL_COMMENTED,
    STORY_LIKED;

    @JsonCreator
    public static NotificationType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
