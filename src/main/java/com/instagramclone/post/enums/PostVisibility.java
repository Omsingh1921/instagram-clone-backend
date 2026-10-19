package com.instagramclone.post.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PostVisibility {
    PUBLIC,
    PRIVATE,
    FOLLOWERS_ONLY;

    public boolean isPublic() {
        return this == PUBLIC;
    }

    @JsonCreator
    public static PostVisibility fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (PostVisibility visibility : values()) {
            if (visibility.name().equalsIgnoreCase(value.trim())) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown PostVisibility: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
