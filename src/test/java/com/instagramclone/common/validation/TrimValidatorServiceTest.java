package com.instagramclone.common.validation;

import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.instagramclone.mail.dto.request.UnsubscribeRequest;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import static org.assertj.core.api.Assertions.assertThat;

class TrimValidatorServiceTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TrimValidator
    static class ProfileTestDto {
        private String displayName;
        private String websiteUrl;
        private String bio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TrimValidator
    static class AuthTestDto {
        private String username;
        private String password;
        private String token;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TrimValidator(exclude = {"customPreserved"})
    static class ExcludedFieldDto {
        private String title;
        private String customPreserved;
    }

    @Test
    @DisplayName("TrimValidator trims and collapses redundant whitespace on standard strings")
    void trimValidator_NormalizesStandardStrings() {
        ProfileTestDto dto = ProfileTestDto.builder()
                .displayName("   John    Doe   ")
                .websiteUrl("  https://instagram.com/john   ")
                .bio("  Software Engineer   ")
                .build();

        validator.validate(dto);

        assertThat(dto.getDisplayName()).isEqualTo("John Doe");
        assertThat(dto.getWebsiteUrl()).isEqualTo("https://instagram.com/john");
        assertThat(dto.getBio()).isEqualTo("Software Engineer");
    }

    @Test
    @DisplayName("TrimValidator preserves newlines and paragraphs in multiline fields")
    void trimValidator_PreservesNewlinesInBio() {
        String multilineBio = "  Line 1\nLine 2\n\nParagraph 2  ";
        ProfileTestDto dto = ProfileTestDto.builder()
                .displayName("John")
                .bio(multilineBio)
                .build();

        validator.validate(dto);

        assertThat(dto.getBio()).isEqualTo("Line 1\nLine 2\n\nParagraph 2");
    }

    @Test
    @DisplayName("TrimValidator strictly preserves passwords, tokens, and OTP codes without mutation")
    void trimValidator_NeverMutatesPasswordsOrTokens() {
        String passwordWithSpaces = "  Secr3t   P@ssword!  ";
        String tokenWithPadding = "  eyJhbGciOi...  ";
        String otpWithZeros = "012345";

        AuthTestDto dto = AuthTestDto.builder()
                .username("   cool_user   ")
                .password(passwordWithSpaces)
                .token(tokenWithPadding)
                .code(otpWithZeros)
                .build();

        validator.validate(dto);

        // Username is trimmed
        assertThat(dto.getUsername()).isEqualTo("cool_user");
        // Security-sensitive fields are strictly untouched
        assertThat(dto.getPassword()).isEqualTo(passwordWithSpaces);
        assertThat(dto.getToken()).isEqualTo(tokenWithPadding);
        assertThat(dto.getCode()).isEqualTo(otpWithZeros);
    }

    @Test
    @DisplayName("TrimValidator honors explicitly excluded fields")
    void trimValidator_HonorsExplicitExclusion() {
        ExcludedFieldDto dto = ExcludedFieldDto.builder()
                .title("   Clean   Title   ")
                .customPreserved("   Do   Not   Touch   ")
                .build();

        validator.validate(dto);

        assertThat(dto.getTitle()).isEqualTo("Clean Title");
        assertThat(dto.getCustomPreserved()).isEqualTo("   Do   Not   Touch   ");
    }

    @Test
    @DisplayName("TrimValidator safely applies to RegisterRequest without corrupting passwords or signup tokens")
    void trimValidator_AppliesToRegisterRequestSafely() {
        com.instagramclone.auth.dto.request.RegisterRequest req = com.instagramclone.auth.dto.request.RegisterRequest.builder()
                .username("   cool_coder   ")
                .displayName("   Cool   Coder   ")
                .email("   test@example.com   ")
                .password("  My P@ssword With Spaces!  ")
                .signupToken("  token_12345  ")
                .build();

        validator.validate(req);

        assertThat(req.getUsername()).isEqualTo("cool_coder");
        assertThat(req.getDisplayName()).isEqualTo("Cool Coder");
        assertThat(req.getEmail()).isEqualTo("test@example.com");
        assertThat(req.getPassword()).isEqualTo("  My P@ssword With Spaces!  ");
        assertThat(req.getSignupToken()).isEqualTo("  token_12345  ");
    }

    @Test
    @DisplayName("TrimValidator safely applies to LoginRequest trimming usernameOrEmail but preserving password")
    void trimValidator_AppliesToLoginRequestSafely() {
        com.instagramclone.auth.dto.request.LoginRequest req = com.instagramclone.auth.dto.request.LoginRequest.builder()
                .usernameOrEmail("   user@example.com   ")
                .password("  Password With Spaces  ")
                .build();

        validator.validate(req);

        assertThat(req.getUsernameOrEmail()).isEqualTo("user@example.com");
        assertThat(req.getPassword()).isEqualTo("  Password With Spaces  ");
    }

    @Test
    @DisplayName("TrimValidator safely preserves caption paragraphs in UpdatePostRequest")
    void trimValidator_AppliesToUpdatePostRequestSafely() {
        com.instagramclone.post.dto.request.UpdatePostRequest req = com.instagramclone.post.dto.request.UpdatePostRequest.builder()
                .caption("   Amazing Sunset!\n\nShot on iPhone 16 Pro Max.   ")
                .build();

        validator.validate(req);

        assertThat(req.getCaption()).isEqualTo("Amazing Sunset!\n\nShot on iPhone 16 Pro Max.");
    }

    @Test
    @DisplayName("TrimValidator safely applies to newly annotated requests preserving security fields")
    void trimValidator_AppliesToNewRequestsSafely() {
        ChangePasswordRequest changePw = ChangePasswordRequest.builder()
                .currentPassword("  OldPass123!  ")
                .newPassword("  NewPass123!  ")
                .confirmPassword("  NewPass123!  ")
                .build();
        validator.validate(changePw);
        assertThat(changePw.getCurrentPassword()).isEqualTo("  OldPass123!  ");
        assertThat(changePw.getNewPassword()).isEqualTo("  NewPass123!  ");
        assertThat(changePw.getConfirmPassword()).isEqualTo("  NewPass123!  ");

        ResetPasswordRequest resetPw = ResetPasswordRequest.builder()
                .token("  reset_token_64chars  ")
                .newPassword("  NewPass123!  ")
                .build();
        validator.validate(resetPw);
        assertThat(resetPw.getToken()).isEqualTo("  reset_token_64chars  ");
        assertThat(resetPw.getNewPassword()).isEqualTo("  NewPass123!  ");

        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken("  refresh_token_xyz  ")
                .build();
        validator.validate(refreshReq);
        assertThat(refreshReq.getRefreshToken()).isEqualTo("  refresh_token_xyz  ");

        ConfirmEmailChangeRequest confirmReq = ConfirmEmailChangeRequest.builder()
                .token("  confirm_token_123  ")
                .build();
        validator.validate(confirmReq);
        assertThat(confirmReq.getToken()).isEqualTo("  confirm_token_123  ");

        VerifyEmailRequest verifyReq = VerifyEmailRequest.builder()
                .token("  verify_token_123  ")
                .build();
        validator.validate(verifyReq);
        assertThat(verifyReq.getToken()).isEqualTo("  verify_token_123  ");

        UnsubscribeRequest unsubReq = UnsubscribeRequest.builder()
                .signature("  signature_hmac  ")
                .build();
        validator.validate(unsubReq);
        assertThat(unsubReq.getSignature()).isEqualTo("  signature_hmac  ");
    }
}
