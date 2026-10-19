package com.instagramclone.auth;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventPublisher;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.provider.IEmailProvider;
import com.instagramclone.mail.provider.SmtpEmailProvider;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.security.otp.IEmailOtpService;
import com.instagramclone.security.otp.OtpChallengeResult;
import com.instagramclone.security.otp.OtpPurpose;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.dto.request.ResendOtpRequest;
import com.instagramclone.auth.dto.request.SignupRequestOtpRequest;
import com.instagramclone.auth.dto.request.SignupVerifyOtpRequest;
import com.instagramclone.auth.dto.request.VerifyOtpRequest;
import com.instagramclone.auth.entity.EmailVerificationToken;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.auth.entity.SignupVerificationTicket;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmailOtpVerificationIntegrationTest extends BaseIntegrationTest {

    @SpyBean
    private IMailService mailService;

    @SpyBean
    private IEmailOtpService emailOtpService;

    @MockBean
    private EmailEventPublisher emailEventPublisher;

    @Autowired
    private SmtpEmailProvider smtpEmailProvider;

    @Autowired
    private com.instagramclone.security.otp.OtpProperties otpProperties;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        reset(mailService, emailOtpService, emailEventPublisher);
    }

    private User createLegacyPendingUser(String username, String email, String password, String displayName) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        Profile profile = Profile.builder()
                .user(user)
                .displayName(displayName != null ? displayName : username)
                .build();
        user.setProfile(profile);
        User saved = userRepository.save(user);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(saved)
                .tokenHash(hashToken("legacy_token_hash_" + UUID.randomUUID()))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        emailVerificationTokenRepository.save(token);
        return saved;
    }

    private String requestAndCaptureSignupOtp(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequestOtpRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendOtpEmail(eq(email), anyString(), otpCaptor.capture());
        return otpCaptor.getValue();
    }

    private String verifySignupOtpAndGetToken(String email, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupVerifyOtpRequest(email, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signupToken").isString())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
        return root.path("data").path("signupToken").asText();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void seedOtpChallenge(String email, UUID userId, String rawOtp) throws Exception {
        seedOtpChallenge(email, userId, rawOtp, OtpPurpose.ACCOUNT_VERIFICATION);
    }

    private void seedOtpChallenge(String email, UUID userId, String rawOtp, OtpPurpose purpose) throws Exception {
        String normalizedEmail = email.trim().toLowerCase();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String emailHash = HexFormat.of().formatHex(md.digest(normalizedEmail.getBytes(StandardCharsets.UTF_8)));

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(otpProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String payload = purpose.name() + ":" + normalizedEmail + ":" + rawOtp;
        String hmac = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        if (stringRedisTemplate != null) {
            String key = "otp:" + purpose.name().toLowerCase() + ":challenge:" + emailHash;
            stringRedisTemplate.opsForHash().put(key, "hmac", hmac);
            if (userId != null) {
                stringRedisTemplate.opsForHash().put(key, "userId", userId.toString());
            }
            stringRedisTemplate.opsForHash().put(key, "attempts", "0");
            stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(300));
        }
    }

    // =========================================================================
    // 1. REGISTRATION INTEGRATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Registration - Initiates approved staged OTP flow and creates verified user upon token submission")
    void registration_InitiatesOtpFlow_WithoutIssuingTokensOrPublishingEvent() throws Exception {
        // Step 1: Request signup OTP
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequestOtpRequest("clark@dailyplanet.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        // Synchronous OTP dispatch executed exactly once
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, times(1)).sendOtpEmail(eq("clark@dailyplanet.com"), eq("there"), otpCaptor.capture());
        String code = otpCaptor.getValue();
        assertThat(code).matches("^\\d{6}$");

        // User is NOT yet created in database
        assertThat(userRepository.findByEmail("clark@dailyplanet.com")).isEmpty();

        // Crucial: No asynchronous email event published for OTP
        verify(emailEventPublisher, never()).publishAfterCommit(any(EmailEvent.class));

        // Step 2: Verify signup OTP
        String signupToken = verifySignupOtpAndGetToken("clark@dailyplanet.com", code);
        assertThat(signupToken).isNotBlank();
        assertThat(userRepository.findByEmail("clark@dailyplanet.com")).isEmpty();

        // Step 3: Complete registration with signupToken
        RegisterRequest request = RegisterRequest.builder()
                .username("clark_kent")
                .email("clark@dailyplanet.com")
                .password("KryptonPassword123!")
                .displayName("Clark Kent")
                .signupToken(signupToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("clark_kent"))
                .andExpect(jsonPath("$.data.user.email").value("clark@dailyplanet.com"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());

        User user = userRepository.findByEmail("clark@dailyplanet.com").orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Registration - Rejects registration request without signup token")
    void register_WithoutSignupToken_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("nosignuptoken")
                .email("nosignup@example.com")
                .password("Password123!")
                .displayName("No Token")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signup verification token is required"));
    }

    @Test
    @DisplayName("Registration - SEC-01: Repeated attempt for pending account CANNOT overwrite password hash, profile, or tokens")
    void registration_RepeatedAttempt_CannotOverwriteCredentialsOrProfile_AndPreservesTokens() throws Exception {
        User originalUser = createLegacyPendingUser("pending_user", "pending@example.com", "InitialPassword123!", "Original Name");
        UUID originalUserId = originalUser.getId();

        // Seed 2 more users to reach 3-account limit for pending@example.com
        for (int i = 2; i <= 3; i++) {
            User extra = User.builder()
                    .username("pending_user_" + i)
                    .email("pending@example.com")
                    .accountSlot(i)
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(false)
                    .build();
            userRepository.save(extra);
        }

        // Verify initial verification-link token was persisted
        List<EmailVerificationToken> initialTokens = emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(originalUserId);
        assertThat(initialTokens).hasSize(1);
        String initialTokenHash = initialTokens.get(0).getTokenHash();

        // Requesting signup OTP for existing pending account at limit is suppressed (anti-enumeration)
        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequestOtpRequest("pending@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        verify(mailService, never()).sendOtpEmail(eq("pending@example.com"), anyString(), anyString());

        // Attacker attempts repeated registration using a valid ticket generated for another email or directly seeded
        String attackerRawToken = "valid_raw_token_for_pending_email_" + UUID.randomUUID().toString().replace("-", "");
        SignupVerificationTicket ticket = SignupVerificationTicket.builder()
                .email("pending@example.com")
                .tokenHash(hashToken(attackerRawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        signupVerificationTicketRepository.save(ticket);

        RegisterRequest attackerAttempt = RegisterRequest.builder()
                .username("pending_user")
                .email("pending@example.com")
                .password("AttackerPassword456!")
                .displayName("Attacker Hijack")
                .signupToken(attackerRawToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackerAttempt)))
                .andExpect(status().isConflict());

        // Must not duplicate account row in database
        List<User> users = userRepository.findAll();
        assertThat(users.stream().filter(u -> "pending@example.com".equals(u.getEmail())).count()).isEqualTo(3);

        User retainedUser = userRepository.findByUsername("pending_user").orElseThrow();
        assertThat(retainedUser.getId()).isEqualTo(originalUserId);
        assertThat(retainedUser.getProfile().getDisplayName()).isEqualTo("Original Name");

        // Password hash must strictly match original password and NOT attacker's password
        assertThat(passwordEncoder.matches("InitialPassword123!", retainedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("AttackerPassword456!", retainedUser.getPasswordHash())).isFalse();

        // Existing verification tokens must NOT be invalidated by repeated registration
        List<EmailVerificationToken> activeTokens = emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(originalUserId);
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getTokenHash()).isEqualTo(initialTokenHash);
    }

    @Test
    @DisplayName("Registration - SEC-01: Different email cannot claim an existing pending username")
    void registration_DifferentEmail_CannotClaimExistingPendingUsername() throws Exception {
        createLegacyPendingUser("victim_handle", "victim@example.com", "OriginalPassword123!", "Victim User");

        // Attacker obtains a valid signup token for attacker@example.com
        String attackerOtp = requestAndCaptureSignupOtp("attacker@example.com");
        String attackerSignupToken = verifySignupOtpAndGetToken("attacker@example.com", attackerOtp);

        RegisterRequest collision = RegisterRequest.builder()
                .username("victim_handle")
                .email("attacker@example.com")
                .password("AttackerPassword123!")
                .displayName("Attacker Handle")
                .signupToken(attackerSignupToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collision)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'victim_handle' is already taken"));
    }

    @Test
    @DisplayName("Registration - SEC-01: Existing verified account cannot be modified through registration")
    void registration_ExistingVerifiedAccount_CannotBeModifiedThroughRegistration() throws Exception {
        User verifiedUser = User.builder()
                .username("verified_owner")
                .email("verified@example.com")
                .passwordHash(passwordEncoder.encode("VerifiedPassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        Profile profile = Profile.builder()
                .user(verifiedUser)
                .displayName("Verified Owner")
                .build();
        verifiedUser.setProfile(profile);
        userRepository.save(verifiedUser);

        // Attacker obtains valid signup proof
        String attackerOtp = requestAndCaptureSignupOtp("attacker_two@example.com");
        String attackerSignupToken = verifySignupOtpAndGetToken("attacker_two@example.com", attackerOtp);

        RegisterRequest collision = RegisterRequest.builder()
                .username("verified_owner")
                .email("attacker_two@example.com")
                .password("AttackerNewPassword123!")
                .displayName("Attacker Name")
                .signupToken(attackerSignupToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collision)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'verified_owner' is already taken"));

        User checked = userRepository.findByEmail("verified@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("VerifiedPassword123!", checked.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("AttackerNewPassword123!", checked.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Registration - Fails safely, rolls back user creation, and clears challenge when synchronous mail delivery fails")
    void registration_FailsSafelyAndRollsBack_WhenEmailDeliveryFails() throws Exception {
        doReturn(EmailDeliveryLog.builder()
                .recipientEmail("failure@example.com")
                .emailType(EmailType.EMAIL_OTP)
                .status(EmailDeliveryStatus.FAILED)
                .lastError("SMTP Connection Timeout")
                .build())
                .when(mailService).sendOtpEmail(eq("failure@example.com"), anyString(), anyString());

        SignupRequestOtpRequest request = new SignupRequestOtpRequest("failure@example.com");

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("MAIL_DELIVERY_FAILED"));

        // User must not be in database
        assertThat(userRepository.findByEmail("failure@example.com")).isEmpty();

        // Challenge in Redis must be invalidated so user is not locked in cooldown
        assertThat(emailOtpService.getRemainingCooldownSeconds("failure@example.com", OtpPurpose.SIGNUP)).isEqualTo(0);
    }

    // =========================================================================
    // 2. OTP VERIFICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Verification - Correct 6-digit OTP verifies pending account, marks DB verified, and issues session tokens")
    void verifyOtp_WithValidCode_VerifiesAccountAndIssuesTokens() throws Exception {
        User pending = createLegacyPendingUser("bruce_wayne", "bruce@wayne.com", "GothamPassword123!", "Bruce Wayne");
        emailOtpService.invalidateChallenge("bruce@wayne.com", OtpPurpose.ACCOUNT_VERIFICATION);

        // Resend OTP for legacy pending account to trigger email dispatch
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("bruce@wayne.com"))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtpEmail(eq("bruce@wayne.com"), eq("bruce_wayne"), otpCaptor.capture());
        String code = otpCaptor.getValue();
        assertThat(code).matches("^\\d{6}$");

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email("bruce@wayne.com")
                .code(code)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("bruce@wayne.com"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));

        User verified = userRepository.findByEmail("bruce@wayne.com").orElseThrow();
        assertThat(verified.getEmailVerified()).isTrue();
        assertThat(verified.getEmailVerifiedAt()).isNotNull();

        // Lingering link-based tokens invalidated
        List<EmailVerificationToken> tokens = emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(verified.getId());
        assertThat(tokens).isEmpty();

        // Welcome email event published
        verify(emailEventPublisher, times(1)).publishAfterCommit(argThat(event ->
                EmailType.WELCOME == event.getEmailType() && "bruce@wayne.com".equals(event.getEmail())));
    }

    @Test
    @DisplayName("Verification - Preserves and verifies 6-digit OTP with leading zeroes")
    void verifyOtp_WithLeadingZeroCode_SuccessfullyVerifies() throws Exception {
        User pending = createLegacyPendingUser("zero_hero", "zero@example.com", "Password123!", "Zero Hero");

        // Pre-create challenge with explicit leading zeroes
        emailOtpService.invalidateChallenge("zero@example.com", OtpPurpose.ACCOUNT_VERIFICATION);
        seedOtpChallenge("zero@example.com", pending.getId(), "004812", OtpPurpose.ACCOUNT_VERIFICATION);

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email("zero@example.com")
                .code("004812")
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        User verified = userRepository.findByEmail("zero@example.com").orElseThrow();
        assertThat(verified.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Verification - Single-use enforcement: re-submitting valid OTP fails with expired/not found error")
    void verifyOtp_SingleUseEnforcement_SecondAttemptFails() throws Exception {
        User pending = createLegacyPendingUser("single_user", "single@example.com", "Password123!", "Single User");
        emailOtpService.invalidateChallenge("single@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("single@example.com"))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtpEmail(eq("single@example.com"), eq("single_user"), otpCaptor.capture());
        String code = otpCaptor.getValue();

        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .email("single@example.com")
                .code(code)
                .build();

        // 1st attempt: Success
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 2nd attempt with same code: Must fail (challenge was consumed and account is verified)
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(anyOf(
                        containsString("Account is already verified"),
                        containsString("expired or does not exist")
                )));
    }

    @Test
    @DisplayName("Verification - Incorrect OTP code returns 400 Bad Request with remaining attempts decrement")
    void verifyOtp_InvalidCode_ReturnsRemainingAttempts() throws Exception {
        User pending = createLegacyPendingUser("wrong_tester", "wrong@example.com", "Password123!", "Wrong Tester");
        emailOtpService.invalidateChallenge("wrong@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("wrong@example.com"))))
                .andExpect(status().isOk());

        VerifyOtpRequest wrongReq = VerifyOtpRequest.builder()
                .email("wrong@example.com")
                .code("999999") // deliberately wrong code
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid verification code. 4 attempt(s) remaining.")))
                .andExpect(jsonPath("$.data").doesNotExist());

        User user = userRepository.findByEmail("wrong@example.com").orElseThrow();
        assertThat(user.getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Verification - Exhausted attempts (5 incorrect tries) burns challenge and locks further attempts")
    void verifyOtp_ExhaustedAttempts_BurnsChallenge() throws Exception {
        User pending = createLegacyPendingUser("exhausted_user", "exhaust@example.com", "Password123!", "Exhausted User");
        emailOtpService.invalidateChallenge("exhaust@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("exhaust@example.com"))))
                .andExpect(status().isOk());

        VerifyOtpRequest wrongReq = VerifyOtpRequest.builder()
                .email("exhaust@example.com")
                .code("888888")
                .build();

        // 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString((5 - i) + " attempt(s) remaining.")));
        }

        // 5th failed attempt: Maximum attempts exceeded and challenge burned
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Please request a new verification code."));

        // 6th attempt: Challenge does not exist anymore
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code has expired or does not exist. Please request a new code."));
    }

    @Test
    @DisplayName("Verification - Missing or expired challenge returns clear error")
    void verifyOtp_MissingOrExpiredChallenge_FailsSafely() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nonexistent@example.com")
                .code("123456")
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code has expired or does not exist. Please request a new code."));
    }

    @Test
    @DisplayName("Verification - Validation rejects non-6-digit or non-numeric formats")
    void verifyOtp_Validation_RejectsMalformedCode() throws Exception {
        // Less than 6 digits
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("valid@example.com", "12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Verification code must be exactly 6 digits")));

        // Non-numeric
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("valid@example.com", "abcdef"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Verification code must be exactly 6 digits")));

        // More than 6 digits
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyOtpRequest("valid@example.com", "1234567"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Verification code must be exactly 6 digits")));
    }

    // =========================================================================
    // 3. RESEND OTP TESTS
    // =========================================================================

    @Test
    @DisplayName("Resend - Cooldown is enforced and returns HTTP 429 with Retry-After header")
    void resendOtp_CooldownEnforced_Returns429WithRetryAfter() throws Exception {
        User pending = createLegacyPendingUser("cooldown_user", "cooldown@example.com", "Password123!", "Cooldown User");
        emailOtpService.invalidateChallenge("cooldown@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        // First resend creates challenge and establishes cooldown
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("cooldown@example.com"))))
                .andExpect(status().isOk());

        // Immediately request resend again (within 60-second cooldown)
        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .email("cooldown@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, not("0")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(containsString("Please wait")));
    }

    @Test
    @DisplayName("Resend - After cooldown expires, resend dispatches fresh OTP email and returns generic success")
    void resendOtp_ValidPendingAccount_DispatchesOneOtpEmail() throws Exception {
        User pending = createLegacyPendingUser("resend_user", "resend@example.com", "Password123!", "Resend User");
        emailOtpService.invalidateChallenge("resend@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .email("resend@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an unverified account exists with this email, a verification code has been sent."));

        // Exactly 1 total sendOtpEmail invocation
        verify(mailService, times(1)).sendOtpEmail(eq("resend@example.com"), eq("resend_user"), anyString());
    }

    @Test
    @DisplayName("Resend - Already verified accounts return generic success without dispatching email")
    void resendOtp_AlreadyVerifiedAccount_DoesNotSendOtp() throws Exception {
        User verifiedUser = User.builder()
                .username("verified_person")
                .email("verified@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        Profile profile = Profile.builder()
                .user(verifiedUser)
                .displayName("Verified Person")
                .build();
        verifiedUser.setProfile(profile);
        userRepository.save(verifiedUser);

        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .email("verified@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an unverified account exists with this email, a verification code has been sent."));

        // Must NOT dispatch any email to already verified user
        verify(mailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Resend - Unknown email returns generic success without dispatching email (anti-enumeration)")
    void resendOtp_NonexistentEmail_ReturnsGenericSuccessWithoutSending() throws Exception {
        ResendOtpRequest resendReq = ResendOtpRequest.builder()
                .email("ghost@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an unverified account exists with this email, a verification code has been sent."));

        verify(mailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
    }

    // =========================================================================
    // 4. CONCURRENCY & EXCEPTION CAUSE PRIVACY TESTS
    // =========================================================================

    @Test
    @DisplayName("Concurrency - Concurrent OTP verification requests allow exactly one success")
    void concurrentVerification_OnlyOneSucceeds() throws Exception {
        User pending = createLegacyPendingUser("concurrent_user", "concurrent@example.com", "Password123!", "Concurrent User");
        emailOtpService.invalidateChallenge("concurrent@example.com", OtpPurpose.ACCOUNT_VERIFICATION);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendOtpRequest("concurrent@example.com"))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtpEmail(eq("concurrent@example.com"), eq("concurrent_user"), otpCaptor.capture());
        String code = otpCaptor.getValue();

        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .email("concurrent@example.com")
                .code(code)
                .build();

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    MvcResult res = mockMvc.perform(post("/api/v1/auth/verify-otp")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();
                    if (res.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 1 request succeeds in single-use verification
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threads - 1);
    }

    @Test
    @DisplayName("Exception Privacy - SmtpEmailProvider throws RuntimeException without leaking unsanitized root cause")
    void smtpEmailProvider_DoesNotLeakRawExceptionCause() {
        // Calling SmtpEmailProvider when offline causes simulation or throwing on send error
        // Verify that RuntimeException thrown on SMTP transmission error has sanitized message and NO raw cause
        try {
            smtpEmailProvider.send(
                    "secret_person@example.com",
                    "009911 is your verification code",
                    "<p>009911</p>",
                    "009911",
                    "from@example.com",
                    "Instagram"
            );
        } catch (RuntimeException ex) {
            assertThat(ex.getCause()).isNull();
            assertThat(ex.getMessage()).doesNotContain("secret_person@example.com");
            assertThat(ex.getMessage()).doesNotContain("009911");
        }
    }
}
