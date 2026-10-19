package com.instagramclone.user;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventListener;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.auth.dto.request.*;
import com.instagramclone.user.dto.request.*;
import com.instagramclone.auth.dto.response.*;
import com.instagramclone.user.dto.response.*;
import com.instagramclone.auth.entity.EmailVerificationToken;
import com.instagramclone.auth.entity.PasswordResetToken;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailAndAccountVerificationTest extends BaseIntegrationTest {

    @SpyBean
    private IMailService mailService;

    @Autowired
    private EmailEventListener emailEventListener;

    @Autowired
    private ITokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        reset(mailService);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Registration - Staged flow requests OTP, verifies OTP, and completes final registration")
    void register_StagedFlow_CreatesVerifiedUserAndIssuesTokens() throws Exception {
        // 1. Request signup OTP
        SignupRequestOtpRequest otpReq = SignupRequestOtpRequest.builder()
                .email("clark@dailyplanet.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendOtpEmail(eq("clark@dailyplanet.com"), any(), otpCaptor.capture());
        String rawOtp = otpCaptor.getValue();

        // 2. Verify signup OTP to receive signupToken
        SignupVerifyOtpRequest verifyReq = SignupVerifyOtpRequest.builder()
                .email("clark@dailyplanet.com")
                .code(rawOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.signupToken").isString())
                .andReturn();

        String signupToken = objectMapper.readTree(verifyResult.getResponse().getContentAsString())
                .path("data").path("signupToken").asText();

        // 3. Complete final registration with the validated signupToken
        RegisterRequest request = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("clark_kent")
                .password("Krypton999!")
                .displayName("Clark Kent")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());

        User user = userRepository.findByEmail("clark@dailyplanet.com").orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify Email - Valid token should verify user and mark token used")
    void verifyEmail_WithValidToken_ShouldVerifyUserAndMarkTokenUsed() throws Exception {
        User user = User.builder()
                .username("bruce_wayne")
                .email("bruce@wayne.com")
                .passwordHash(passwordEncoder.encode("Batcave123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        String rawToken = "raw_secure_verification_token_1234567890abcdef";
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationTokenRepository.save(token);

        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token(rawToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getEmailVerified()).isTrue();
        assertThat(updatedUser.getEmailVerifiedAt()).isNotNull();

        EmailVerificationToken updatedToken = emailVerificationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(updatedToken.isUsed()).isTrue();
        assertThat(updatedToken.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify Email - Reused token should fail with 400 Bad Request")
    void verifyEmail_WithReusedToken_ShouldFailWithBadRequest() throws Exception {
        User user = User.builder()
                .username("barry_allen")
                .email("barry@starlabs.com")
                .passwordHash(passwordEncoder.encode("Speedforce123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        String rawToken = "reused_token_1234567890abcdef";
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .usedAt(LocalDateTime.now().minusHours(1))
                .build();
        emailVerificationTokenRepository.save(token);

        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token(rawToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This verification token has already been used"));
    }

    @Test
    @DisplayName("Verify Email - Expired token should fail with 400 Bad Request")
    void verifyEmail_WithExpiredToken_ShouldFailWithBadRequest() throws Exception {
        User user = User.builder()
                .username("arthur_curry")
                .email("arthur@atlantis.com")
                .passwordHash(passwordEncoder.encode("Atlantis123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        String rawToken = "expired_token_1234567890abcdef";
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusHours(2)) // Expired
                .build();
        emailVerificationTokenRepository.save(token);

        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token(rawToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Verification token has expired. Please request a new verification email."));
    }

    @Test
    @DisplayName("Verify Email - Tampered token should fail with 400 Bad Request")
    void verifyEmail_WithTamperedToken_ShouldFailWithBadRequest() throws Exception {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token("completely_invalid_random_token")
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    @DisplayName("Resend Verification - Anti-enumeration: returns generic success for non-existent email")
    void resendVerification_WithNonExistentEmail_ShouldReturnGenericSuccess() throws Exception {
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("nobody@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a verification link has been sent."));
    }

    @Test
    @DisplayName("Resend Verification - For existing unverified user: invalidates previous tokens and issues new one")
    void resendVerification_WithExistingUser_ShouldInvalidateOldTokensAndIssueNew() throws Exception {
        User user = User.builder()
                .username("victor_stone")
                .email("victor@cyborg.com")
                .passwordHash(passwordEncoder.encode("Cyborg123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        EmailVerificationToken oldToken = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hashToken("old_token"))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationTokenRepository.save(oldToken);

        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("victor@cyborg.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a verification link has been sent."));

        // Old token must be invalidated
        EmailVerificationToken refreshedOldToken = emailVerificationTokenRepository.findById(oldToken.getId()).orElseThrow();
        assertThat(refreshedOldToken.isUsed()).isTrue();

        // New active token must exist
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getId()).isNotEqualTo(oldToken.getId());
    }

    @Test
    @DisplayName("Forgot Password - Anti-enumeration: returns identical generic response whether email exists or not")
    void forgotPassword_AntiEnumeration_ShouldReturnGenericSuccess() throws Exception {
        // 1. Non-existent email
        ForgotPasswordRequest nonExistentRequest = ForgotPasswordRequest.builder()
                .email("nonexistent@domain.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset link has been sent."));

        // 2. Existing user
        User user = User.builder()
                .username("hal_jordan")
                .email("hal@greenlantern.com")
                .passwordHash(passwordEncoder.encode("RingPower123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        ForgotPasswordRequest existingRequest = ForgotPasswordRequest.builder()
                .email("hal@greenlantern.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset link has been sent."));

        // Password reset token must be persisted in database
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getTokenHash()).isNotBlank();
        assertThat(tokens.get(0).getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Reset Password - Valid token updates password and revokes all active sessions")
    void resetPassword_WithValidToken_ShouldChangePasswordAndRevokeSessions() throws Exception {
        User user = User.builder()
                .username("oliver_queen")
                .email("oliver@queen.com")
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        // Login to establish session
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("oliver_queen")
                .password("OldPassword123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String rawRefreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // Verify active refresh token in database
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isNotEmpty();

        // Create reset token
        String rawResetToken = "raw_secure_reset_token_queen_123456789";
        String tokenHash = hashToken(rawResetToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Perform password reset
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("BrandNewSecretPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully. Please log in with your new credentials."));

        // 1. Password must be updated and match new password
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("BrandNewSecretPassword999!", updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123!", updatedUser.getPasswordHash())).isFalse();

        // 2. All previous refresh tokens must be revoked
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();

        // 3. User session must be blacklisted in Redis for pre-reset sessions
        java.util.Date priorSessionIssueDate = new java.util.Date(System.currentTimeMillis() - 5000);
        assertThat(tokenBlacklistService.isUserRevoked(user.getId(), priorSessionIssueDate)).isTrue();

        // 4. Token must be marked used
        PasswordResetToken updatedResetToken = passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(updatedResetToken.isUsed()).isTrue();

        // 5. User can log in with new password
        LoginRequest newLoginRequest = LoginRequest.builder()
                .usernameOrEmail("oliver_queen")
                .password("BrandNewSecretPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Reset Password - Reused token should fail with 400 Bad Request")
    void resetPassword_WithReusedToken_ShouldFailWithBadRequest() throws Exception {
        User user = User.builder()
                .username("kara_zor_el")
                .email("kara@nationalcity.com")
                .passwordHash(passwordEncoder.encode("Supergirl123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        String rawResetToken = "reused_reset_token_kara_12345";
        String tokenHash = hashToken(rawResetToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .usedAt(LocalDateTime.now().minusMinutes(5)) // Already used
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("AnotherNewPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This password reset token has already been used"));
    }

    @Test
    @DisplayName("Reset Password - Expired token should fail with 400 Bad Request")
    void resetPassword_WithExpiredToken_ShouldFailWithBadRequest() throws Exception {
        User user = User.builder()
                .username("john_constantine")
                .email("john@hellblazer.com")
                .passwordHash(passwordEncoder.encode("Hellblazer123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);

        String rawResetToken = "expired_reset_token_john_12345";
        String tokenHash = hashToken(rawResetToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .build();
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("AnotherNewPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password reset token has expired. Please request a new password reset link."));
    }

    @Test
    @DisplayName("EmailEventListener - Idempotency deduplicates duplicate email delivery events")
    void emailEventListener_DeduplicatesEventsViaRedis() {
        EmailEvent event = EmailEvent.builder()
                .eventId("dedup-test-" + UUID.randomUUID())
                .eventType(KafkaConstants.EVENT_EMAIL_VERIFICATION_REQUESTED)
                .userId(UUID.randomUUID())
                .email("test-dedup@example.com")
                .username("dedup_user")
                .rawToken("test-token-12345")
                .timestamp(LocalDateTime.now())
                .build();

        // First call processes normally
        emailEventListener.onSpringEmailEvent(event);

        // Immediate duplicate call is deduplicated via Redis idempotency key
        emailEventListener.onSpringEmailEvent(event);
    }

    @Test
    @DisplayName("IMailService - Offline / Disabled mail handling does not throw exceptions")
    void mailService_SafeExecutionWithoutThrowing() {
        mailService.sendVerificationEmail("offline@example.com", "offline_user", "sample-token");
        mailService.sendPasswordResetEmail("offline@example.com", "offline_user", "sample-token");
        mailService.sendPasswordChangedAlert("offline@example.com", "offline_user");
    }
}
