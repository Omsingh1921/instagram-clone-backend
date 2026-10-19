package com.instagramclone.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Frequency options for periodic activity digest notification emails.
 */
public enum DigestFrequency {
    DAILY,
    WEEKLY,
    NEVER;

    public boolean isDaily() {
        return this == DAILY;
    }

    public boolean isWeekly() {
        return this == WEEKLY;
    }

    public boolean isNever() {
        return this == NEVER;
    }

    @JsonCreator
    public static DigestFrequency fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (DigestFrequency frequency : values()) {
            if (frequency.name().equalsIgnoreCase(value.trim())) {
                return frequency;
            }
        }
        throw new IllegalArgumentException("Unknown DigestFrequency: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
