package com.instagramclone.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationConstantsTest {

    private final Pattern usernamePattern = Pattern.compile(ValidationConstants.USERNAME_REGEX);
    private final Pattern emailPattern = Pattern.compile(ValidationConstants.EMAIL_REGEX);
    private final Pattern otpPattern = Pattern.compile(ValidationConstants.OTP_REGEX);
    private final Pattern uuidPattern = Pattern.compile(ValidationConstants.UUID_REGEX);
    private final Pattern sortDirectionPattern = Pattern.compile(ValidationConstants.SORT_DIRECTION_REGEX);

    @ParameterizedTest
    @ValueSource(strings = {"john_doe", "user.name", "user_123", "abc", "a.b.c", "john_doe_official_account_12"})
    @DisplayName("USERNAME_REGEX accepts valid Instagram usernames")
    void usernameRegex_ValidUsernames(String username) {
        assertThat(usernamePattern.matcher(username).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".username",      // starts with dot
            "username.",      // ends with dot
            "user..name",     // consecutive dots
            "ab",             // too short (< 3 chars)
            "this_username_is_way_too_long_for_instagram_31c", // > 30 chars
            "user@name",      // invalid character @
            "user name",      // whitespace
            "user-name"       // hyphen not permitted in Instagram username contract
    })
    @DisplayName("USERNAME_REGEX rejects invalid Instagram usernames")
    void usernameRegex_InvalidUsernames(String username) {
        assertThat(usernamePattern.matcher(username).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "first.last@domain.co.uk", "a+b@c.org"})
    @DisplayName("EMAIL_REGEX accepts valid standard emails")
    void emailRegex_ValidEmails(String email) {
        assertThat(emailPattern.matcher(email).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@missinguser.com", "user@.com", "user@domain", "user@domain..com"})
    @DisplayName("EMAIL_REGEX rejects invalid emails")
    void emailRegex_InvalidEmails(String email) {
        assertThat(emailPattern.matcher(email).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000", "012345", "123456", "999999"})
    @DisplayName("OTP_REGEX accepts exact 6 numeric digits including leading zeros")
    void otpRegex_ValidOtps(String otp) {
        assertThat(otpPattern.matcher(otp).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12345a", "abcdef", "      "})
    @DisplayName("OTP_REGEX rejects malformed OTP codes")
    void otpRegex_InvalidOtps(String otp) {
        assertThat(otpPattern.matcher(otp).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123e4567-e89b-12d3-a456-426614174000",
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff"
    })
    @DisplayName("UUID_REGEX accepts standard RFC canonical hex format")
    void uuidRegex_ValidUuids(String uuid) {
        assertThat(uuidPattern.matcher(uuid).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-uuid",
            "123e4567-e89b-12d3-a456",
            "123e4567-e89b-12d3-a456-4266141740001",
            "123e4567_e89b_12d3_a456_426614174000"
    })
    @DisplayName("UUID_REGEX rejects non-UUID strings")
    void uuidRegex_InvalidUuids(String uuid) {
        assertThat(uuidPattern.matcher(uuid).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASC", "DESC", "asc", "desc"})
    @DisplayName("SORT_DIRECTION_REGEX accepts ASC and DESC case-insensitively")
    void sortDirectionRegex_Valid(String dir) {
        assertThat(sortDirectionPattern.matcher(dir).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASCENDING", "random", "DROP TABLE", "1"})
    @DisplayName("SORT_DIRECTION_REGEX rejects non-standard directions")
    void sortDirectionRegex_Invalid(String dir) {
        assertThat(sortDirectionPattern.matcher(dir).matches()).isFalse();
    }

    @Test
    @DisplayName("Constants have expected values")
    void constantsValues() {
        assertThat(ValidationConstants.USERNAME_MIN_LENGTH).isEqualTo(3);
        assertThat(ValidationConstants.USERNAME_MAX_LENGTH).isEqualTo(30);
        assertThat(ValidationConstants.PASSWORD_MIN_LENGTH).isEqualTo(8);
        assertThat(ValidationConstants.PASSWORD_MAX_LENGTH).isEqualTo(100);
        assertThat(ValidationConstants.OTP_LENGTH).isEqualTo(6);
        assertThat(ValidationConstants.MAX_IMAGE_PIXELS).isEqualTo(40_000_000L);
    }
}
