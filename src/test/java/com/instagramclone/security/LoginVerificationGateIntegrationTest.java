package com.instagramclone.security;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RefreshTokenRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.dto.request.VerifyEmailRequest;
import com.instagramclone.auth.dto.request.VerifyOtpRequest;
import com.instagramclone.auth.entity.EmailVerificationToken;
import com.instagramclone.auth.entity.RefreshToken;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Phase 4 — Login & JWT Email Verification Gate Integration Tests")
public class LoginVerificationGateIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private IMailService mailService;

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        doReturn(EmailDeliveryLog.builder()
                .recipientEmail("test@example.com")
                .emailType(EmailType.EMAIL_OTP)
                .status(EmailDeliveryStatus.SENT_TO_PROVIDER)
                .build())
                .when(mailService).sendOtpEmail(anyString(), anyString(), anyString());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Test
    @DisplayName("Login Gate - Unverified account with correct password returns 403 EMAIL_NOT_VERIFIED and issues no tokens")
    void login_UnverifiedAccount_WithCorrectPassword_ReturnsForbiddenAndNoTokens() throws Exception {
        User user = userRepository.save(User.builder()
                .username("alice_gate")
                .email("alice@gate.com")
                .passwordHash(passwordEncoder.encode("ValidPassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        assertThat(user.getEmailVerified()).isFalse();

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("alice_gate")
                .password("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message").value("Please verify your email address before logging in."));

        // Persisted database assertion: Zero refresh tokens must exist for rejected user
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("Login Gate - Unverified account with wrong password retains generic 401 INVALID_CREDENTIALS")
    void login_UnverifiedAccount_WithWrongPassword_ReturnsUnauthorizedInvalidCredentials() throws Exception {
        userRepository.save(User.builder()
                .username("bob_gate")
                .email("bob@gate.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        LoginRequest badLogin = LoginRequest.builder()
                .usernameOrEmail("bob_gate")
                .password("TotallyWrongPass999!")
                .build();

        // Must return 401 INVALID_CREDENTIALS, preserving timing & anti-enumeration protections
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Login Gate - Verified account can log in normally and receives tokens")
    void login_VerifiedAccount_WithCorrectPassword_SucceedsAndIssuesTokens() throws Exception {
        User verifiedUser = userRepository.save(User.builder()
                .username("charlie_gate")
                .email("charlie@gate.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("charlie_gate")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.username").value("charlie_gate"));

        // Persisted database assertion: Active refresh token exists in DB
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(verifiedUser.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("Login Gate - Login succeeds after successful OTP verification")
    void login_AfterSuccessfulOtpVerification_Succeeds() throws Exception {
        User user = userRepository.save(User.builder()
                .username("david_otp")
                .email("david@gate.com")
                .passwordHash(passwordEncoder.encode("DavidPass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        var challenge = emailOtpService.createOtpChallenge("david@gate.com", user.getId(), com.instagramclone.security.otp.OtpPurpose.ACCOUNT_VERIFICATION);
        String generatedOtp = challenge.getRawOtp();
        assertThat(generatedOtp).isNotBlank();

        // 1. Prior to verification, login is blocked
        LoginRequest loginAttempt1 = LoginRequest.builder()
                .usernameOrEmail("david_otp")
                .password("DavidPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0]").value("EMAIL_NOT_VERIFIED"));

        // 2. Complete OTP verification
        VerifyOtpRequest verifyOtp = VerifyOtpRequest.builder()
                .email("david@gate.com")
                .code(generatedOtp)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Post-verification, standard login succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("Login Gate - Login succeeds after successful legacy link verification")
    void login_AfterSuccessfulLegacyLinkVerification_Succeeds() throws Exception {
        User eve = userRepository.save(User.builder()
                .username("eve_link")
                .email("eve@gate.com")
                .passwordHash(passwordEncoder.encode("EvePassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        assertThat(eve.getEmailVerified()).isFalse();

        // Prior to link verification, login is blocked
        LoginRequest loginReq = LoginRequest.builder()
                .usernameOrEmail("eve_link")
                .password("EvePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0]").value("EMAIL_NOT_VERIFIED"));

        // Seed a valid link verification token
        String rawToken = "raw_verification_token_eve_987654321";
        String tokenHash = hashToken(rawToken);
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .user(eve)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build());

        // Verify email using link token
        VerifyEmailRequest verifyEmailReq = VerifyEmailRequest.builder()
                .token(rawToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyEmailReq)))
                .andExpect(status().isOk());

        // Now login succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Refresh-Token Gate - Unverified account's refresh attempt is rejected and token is revoked in DB")
    void refreshToken_UnverifiedAccount_FailsAndRevokesPresentedTokenInDatabase() throws Exception {
        User unverifiedUser = userRepository.save(User.builder()
                .username("frank_refresh")
                .email("frank@gate.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        String rawRefreshToken = "raw_refresh_token_frank_123456789";
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken seedToken = refreshTokenRepository.save(RefreshToken.builder()
                .user(unverifiedUser)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(rawRefreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email address is not verified"));

        // Persisted database assertion: Token must be revoked in DB despite exception thrown
        RefreshToken reloaded = refreshTokenRepository.findById(seedToken.getId()).orElseThrow();
        assertThat(reloaded.getRevokedAt()).isNotNull();
        assertThat(reloaded.isValid()).isFalse();

        // Attempting refresh again must now fail with token expired/revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is expired or revoked"));
    }

    @Test
    @DisplayName("HTTP JWT Gate - Unverified user's access token is rejected on protected endpoints")
    void httpJwtFilter_UnverifiedUserToken_RejectsProtectedEndpoint() throws Exception {
        User unverifiedUser = userRepository.save(User.builder()
                .username("grace_jwt")
                .email("grace@gate.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        String unverifiedToken = jwtTokenProvider.generateTokenForUser(
                unverifiedUser.getId(), unverifiedUser.getUsername(), unverifiedUser.getEmail());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + unverifiedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("WebSocket/STOMP Gate - Unverified user's JWT is rejected at STOMP CONNECT")
    void webSocket_UnverifiedUserToken_RejectsStompConnect() {
        User unverifiedUser = userRepository.save(User.builder()
                .username("heidi_ws")
                .email("heidi@gate.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build());

        String unverifiedToken = jwtTokenProvider.generateTokenForUser(
                unverifiedUser.getId(), unverifiedUser.getUsername(), unverifiedUser.getEmail());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + unverifiedToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Unauthorized: Email address is not verified");
    }

    @Test
    @DisplayName("WebSocket/STOMP Gate - Verified user's JWT establishes authenticated STOMP CONNECT")
    void webSocket_VerifiedUserToken_AllowsStompConnect() {
        User verifiedUser = userRepository.save(User.builder()
                .username("ivan_ws")
                .email("ivan@gate.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build());

        String verifiedToken = jwtTokenProvider.generateTokenForUser(
                verifiedUser.getId(), verifiedUser.getUsername(), verifiedUser.getEmail());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + verifiedToken);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = webSocketAuthInterceptor.preSend(message, mock(MessageChannel.class));
        assertThat(result).isNotNull();
    }
}
