package com.instagramclone.search.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchType {
    USER,
    HASHTAG,
    KEYWORD;

    @JsonCreator
    public static SearchType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return KEYWORD;
        }
        for (SearchType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SearchType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
