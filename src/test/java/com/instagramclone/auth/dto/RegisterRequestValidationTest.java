package com.instagramclone.auth.dto;

import com.instagramclone.auth.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static final String VALID_SIGNUP_TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private RegisterRequest buildRequest(String username, String email) {
        return RegisterRequest.builder()
                .signupToken(VALID_SIGNUP_TOKEN)
                .username(username)
                .email(email)
                .password("Password123!")
                .displayName("Valid Display Name")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "john_doe",
            "peter.parker",
            "spidey123",
            "_valid_user_",
            "a.b.c",
            "user_1.2",
            "instagram_clone"
    })
    @DisplayName("Valid Usernames - Should pass validation with proper Instagram regex")
    void validUsernames_PassValidation(String username) {
        RegisterRequest request = buildRequest(username, "user@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".leadingdot",          // starts with period
            "trailingdot.",          // ends with period
            "consecutive..dots",     // consecutive periods
            ".both.dots.",           // starts and ends with period
            "user-hyphen",           // hyphens not allowed in Instagram usernames
            "user space",            // spaces not allowed
            "user@instagram",        // special characters not allowed
            "ab"                     // fewer than 3 characters
    })
    @DisplayName("Invalid Usernames - Should fail validation with proper regex")
    void invalidUsernames_FailValidation(String username) {
        RegisterRequest request = buildRequest(username, "user@example.com");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "john.doe@company.org",
            "spidey+tag@dailybugle.net",
            "name@subdomain.example.co.uk"
    })
    @DisplayName("Valid Emails - Should pass validation with RFC-compliant regex")
    void validEmails_PassValidation(String email) {
        RegisterRequest request = buildRequest("valid_username", email);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid_email",
            "user@",
            "@domain.com",
            "user@domain",         // missing TLD extension
            "user@domain.c",       // TLD shorter than 2 characters
            "user @domain.com"     // space in email
    })
    @DisplayName("Invalid Emails - Should fail validation with proper regex")
    void invalidEmails_FailValidation(String email) {
        RegisterRequest request = buildRequest("valid_username", email);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Oversized Signup Token - Should fail validation")
    void oversizedSignupToken_FailsValidation() {
        RegisterRequest request = RegisterRequest.builder()
                .signupToken("a".repeat(256))
                .username("valid_user")
                .password("Password123!")
                .build();
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("signupToken"));
    }

    @Test
    @DisplayName("Password Length Bounds - Too short or too long should fail")
    void passwordLengthBounds_FailValidation() {
        RegisterRequest shortPw = RegisterRequest.builder()
                .signupToken(VALID_SIGNUP_TOKEN)
                .username("valid_user")
                .password("short")
                .build();
        assertThat(validator.validate(shortPw)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        RegisterRequest longPw = RegisterRequest.builder()
                .signupToken(VALID_SIGNUP_TOKEN)
                .username("valid_user")
                .password("a".repeat(101))
                .build();
        assertThat(validator.validate(longPw)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
