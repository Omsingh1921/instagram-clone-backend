package com.instagramclone.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationType {
    EMAIL,
    PHONE,
    TWO_FACTOR,
    IDENTITY_VERIFICATION;

    @JsonCreator
    public static VerificationType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (VerificationType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown VerificationType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
