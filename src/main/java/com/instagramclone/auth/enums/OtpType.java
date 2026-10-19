package com.instagramclone.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OtpType {
    LOGIN,
    REGISTER,
    FORGET_PASSWORD,
    FORGOT_PASSWORD,
    RESET_PASSWORD,
    EMAIL_CHANGE,
    ACCOUNT_VERIFICATION;

    @JsonCreator
    public static OtpType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (OtpType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OtpType: " + value);
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
