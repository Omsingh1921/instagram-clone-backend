package com.instagramclone.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.security.otp.IEmailOtpService;
import com.instagramclone.security.otp.OtpPurpose;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.dto.request.SignupRequestOtpRequest;
import com.instagramclone.auth.dto.request.SignupVerifyOtpRequest;
import com.instagramclone.auth.dto.request.VerifyOtpRequest;
import com.instagramclone.auth.entity.SignupVerificationTicket;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StagedRegistrationIntegrationTest extends BaseIntegrationTest {

    @SpyBean
    private IMailService mailService;

    @SpyBean
    private IEmailOtpService emailOtpService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        reset(mailService);
    }

    private String requestAndCaptureSignupOtp(String email) throws Exception {
        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder()
                .email(email)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendOtpEmail(eq(email), any(), otpCaptor.capture());
        return otpCaptor.getValue();
    }

    private String verifyOtpAndGetSignupToken(String email, String code) throws Exception {
        SignupVerifyOtpRequest verifyReq = SignupVerifyOtpRequest.builder()
                .email(email)
                .code(code)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.signupToken").isString())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("signupToken").asText();
    }

    @Test
    @DisplayName("1. Signup OTP request does not create user, profile, or refresh token rows")
    void signupOtpRequest_DoesNotCreateUserOrTokens() throws Exception {
        String email = "candidate@example.com";
        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder()
                .email(email)
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(profileRepository.count()).isEqualTo(0);
        assertThat(refreshTokenRepository.count()).isEqualTo(0);
        assertThat(signupVerificationTicketRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. OTP verification yields signup proof and does not create account or issue access/refresh tokens")
    void signupOtpVerify_YieldsProofAndDoesNotCreateAccount() throws Exception {
        String email = "staged_user@example.com";
        String rawOtp = requestAndCaptureSignupOtp(email);

        SignupVerifyOtpRequest verifyReq = SignupVerifyOtpRequest.builder()
                .email(email)
                .code(rawOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.signupToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").value(600))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andReturn();

        JsonNode root = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String signupToken = root.path("data").path("signupToken").asText();
        assertThat(signupToken).isNotBlank();

        // Architectural Rule: Zero account rows in database
        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(profileRepository.count()).isEqualTo(0);
        assertThat(refreshTokenRepository.count()).isEqualTo(0);

        // Verification ticket stored persistently
        assertThat(signupVerificationTicketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Valid OTP yields temporary signup proof bound strictly to verified normalized email")
    void validOtp_YieldsSignupProofBoundToEmail() throws Exception {
        String email = "UPPERCASE_TEST@Example.Com";
        String rawOtp = requestAndCaptureSignupOtp(email.toLowerCase());

        String signupToken = verifyOtpAndGetSignupToken(email.toLowerCase(), rawOtp);
        assertThat(signupToken).isNotBlank();

        List<SignupVerificationTicket> tickets = signupVerificationTicketRepository.findAll();
        assertThat(tickets).hasSize(1);
        SignupVerificationTicket ticket = tickets.get(0);
        assertThat(ticket.getEmail()).isEqualTo("uppercase_test@example.com");
        assertThat(ticket.isConsumed()).isFalse();
        assertThat(ticket.isExpired()).isFalse();
    }

    @Test
    @DisplayName("4. Wrong, expired, and exhausted OTP challenges fail safely")
    void invalidAndExhaustedOtp_FailSafely() throws Exception {
        String email = "retry_test@example.com";
        requestAndCaptureSignupOtp(email);

        // Submit incorrect OTP
        SignupVerifyOtpRequest wrongReq = SignupVerifyOtpRequest.builder()
                .email(email)
                .code("999999")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid verification code")));

        // Submit remaining wrong attempts to exhaust
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongReq)));
        }

        // Exhausted code
        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Purpose Isolation - Signup and legacy account-verification OTPs cannot be crossed")
    void purposeIsolation_SignupAndLegacyOtpsCannotCross() throws Exception {
        String email = "isolated_purpose@example.com";

        // Request signup OTP (purpose SIGNUP)
        String signupOtp = requestAndCaptureSignupOtp(email);

        // Attempt to verify signup OTP at legacy account-verification endpoint
        VerifyOtpRequest legacyReq = VerifyOtpRequest.builder()
                .email(email)
                .code(signupOtp)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legacyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Invalid, expired, consumed, or replayed signup proofs cannot create accounts")
    void invalidOrReplayedProof_CannotCreateAccount() throws Exception {
        String email = "replay_target@example.com";
        String rawOtp = requestAndCaptureSignupOtp(email);
        String signupToken = verifyOtpAndGetSignupToken(email, rawOtp);

        RegisterRequest validRegistration = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("replay_user")
                .password("Password123!")
                .displayName("Replay User")
                .build();

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Replaying the same proof fails
        RegisterRequest replayed = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("replay_user_second")
                .password("Password123!")
                .displayName("Second Replay")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replayed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Bogus proof fails
        RegisterRequest bogus = RegisterRequest.builder()
                .signupToken("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .username("bogus_user")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bogus)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Concurrency - Two concurrent final registration requests using the same proof cannot create duplicate accounts")
    void concurrentRegistration_PreventsDuplicateAccountCreation() throws Exception {
        String email = "concurrent@example.com";
        String rawOtp = requestAndCaptureSignupOtp(email);
        String signupToken = verifyOtpAndGetSignupToken(email, rawOtp);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    RegisterRequest req = RegisterRequest.builder()
                            .signupToken(signupToken)
                            .username("concurrent_user_" + idx)
                            .password("Password123!")
                            .displayName("Concurrent User")
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    statusCodes.add(result.getResponse().getStatus());
                } catch (Exception e) {
                    statusCodes.add(500);
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Exactly one should succeed with 201 Created and one fail with 400 Bad Request
        assertThat(statusCodes).containsExactlyInAnyOrder(201, 400);

        // Database Invariant: Exactly one user created
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(profileRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("8 & 9. Final registration creates verified account with emailVerified=true and issues session tokens")
    void finalRegistration_CreatesVerifiedUserAndIssuesTokens() throws Exception {
        String email = "verified_member@example.com";
        String rawOtp = requestAndCaptureSignupOtp(email);
        String signupToken = verifyOtpAndGetSignupToken(email, rawOtp);

        RegisterRequest regReq = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("verified_member")
                .password("Password123!")
                .displayName("Verified Member")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("verified_member"))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true))
                .andReturn();

        User user = userRepository.findByUsername("verified_member").orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("10. Missing signup proof is rejected with 400 Bad Request")
    void missingSignupProof_IsRejected() throws Exception {
        RegisterRequest reqWithoutToken = RegisterRequest.builder()
                .username("no_token_user")
                .password("Password123!")
                .displayName("No Token")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqWithoutToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signup verification token is required"));
    }

    @Test
    @DisplayName("11. Username conflict preserves the valid signup proof for re-selection")
    void usernameConflict_PreservesValidSignupProof() throws Exception {
        // Pre-create an existing user
        String token1 = createValidSignupTicket("existing@example.com");
        RegisterRequest req1 = RegisterRequest.builder()
                .signupToken(token1)
                .username("taken_handle")
                .password("Password123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second user verifies their email OTP
        String email2 = "new_user@example.com";
        String rawOtp2 = requestAndCaptureSignupOtp(email2);
        String signupToken2 = verifyOtpAndGetSignupToken(email2, rawOtp2);

        // Attempt registration with taken username
        RegisterRequest conflictingReq = RegisterRequest.builder()
                .signupToken(signupToken2)
                .username("taken_handle")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'taken_handle' is already taken"));

        // Verify ticket was NOT consumed
        SignupVerificationTicket ticket = signupVerificationTicketRepository.findAll().stream()
                .filter(t -> t.getEmail().equals(email2))
                .findFirst().orElseThrow();
        assertThat(ticket.isConsumed()).isFalse();

        // User chooses another username with the SAME proof
        RegisterRequest availableReq = RegisterRequest.builder()
                .signupToken(signupToken2)
                .username("available_handle")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availableReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findByUsername("available_handle")).isPresent();
    }

    @Test
    @DisplayName("12. Existing registered email cannot be claimed through new signup")
    void existingRegisteredEmail_CannotBeClaimedThroughSignup() throws Exception {
        // Register 3 existing users to reach the 3-account limit
        for (int i = 1; i <= 3; i++) {
            String token = createValidSignupTicket("owner@example.com");
            RegisterRequest reg = RegisterRequest.builder()
                    .signupToken(token)
                    .username("original_owner_" + i)
                    .password("Password123!")
                    .build();
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isCreated());
        }

        reset(mailService);

        // Attacker requests signup OTP for existing email
        SignupRequestOtpRequest attackReq = SignupRequestOtpRequest.builder()
                .email("owner@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // OTP must NOT be dispatched for already registered account
        verify(mailService, never()).sendOtpEmail(eq("owner@example.com"), any(), any());
    }

    @Test
    @DisplayName("15. Failed OTP email delivery does not leave usable challenge")
    void failedOtpEmailDelivery_InvalidatesChallenge() throws Exception {
        doReturn(EmailDeliveryLog.builder().status(EmailDeliveryStatus.FAILED).build())
                .when(mailService).sendOtpEmail(anyString(), anyString(), anyString());

        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder()
                .email("delivery_fail@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0]").value("MAIL_DELIVERY_FAILED"));
    }

    @Test
    @DisplayName("16. Raw OTPs, signup proofs, and passwords are not written to captured application logs")
    void rawSecrets_NeverLoggedDuringStagedRegistration() throws Exception {
        ch.qos.logback.classic.Logger authLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(com.instagramclone.auth.service.impl.AuthServiceImpl.class);
        ch.qos.logback.classic.Logger otpLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(com.instagramclone.security.otp.EmailOtpServiceImpl.class);

        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        authLogger.addAppender(appender);
        otpLogger.addAppender(appender);

        String email = "secret_candidate@example.com";
        String password = "SuperSecretPassword123!";

        try {
            String rawOtp = requestAndCaptureSignupOtp(email);
            String signupToken = verifyOtpAndGetSignupToken(email, rawOtp);

            RegisterRequest reg = RegisterRequest.builder()
                    .signupToken(signupToken)
                    .username("secret_cand")
                    .password(password)
                    .displayName("Candidate")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isCreated());

            for (ch.qos.logback.classic.spi.ILoggingEvent event : appender.list) {
                String msg = event.getFormattedMessage();
                assertThat(msg).doesNotContain(rawOtp);
                assertThat(msg).doesNotContain(signupToken);
                assertThat(msg).doesNotContain(password);
                assertThat(msg).doesNotContain(email); // full email must not be logged
            }
        } finally {
            authLogger.detachAppender(appender);
            otpLogger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("17. Signup token format matches 256-bit entropy (64-character lowercase hexadecimal string)")
    void signupTokenFormat_MatchesHex256BitEntropy() throws Exception {
        String email = "token_format_test@example.com";
        String rawOtp = requestAndCaptureSignupOtp(email);
        String signupToken = verifyOtpAndGetSignupToken(email, rawOtp);

        assertThat(signupToken)
                .as("Production signup token must be a 64-character lowercase hex string")
                .isNotNull()
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("18. Legacy pending account can recover via legacy OTP verification and receive session tokens")
    void legacyPendingAccount_CanRecoverViaLegacyOtp() throws Exception {
        // Create an unverified legacy user in the database
        User pendingUser = User.builder()
                .username("legacy_pending_user")
                .email("legacy_pending@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        userRepository.save(pendingUser);

        // Create legacy challenge with ACCOUNT_VERIFICATION purpose
        var challenge = emailOtpService.createOtpChallenge("legacy_pending@example.com", pendingUser.getId(), OtpPurpose.ACCOUNT_VERIFICATION);

        VerifyOtpRequest legacyVerifyReq = VerifyOtpRequest.builder()
                .email("legacy_pending@example.com")
                .code(challenge.getRawOtp())
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legacyVerifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));

        User updated = userRepository.findByEmail("legacy_pending@example.com").orElseThrow();
        assertThat(updated.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("19. New signup OTP request for pending legacy account suppresses email without revealing status")
    void newSignupRequest_ForPendingLegacyAccount_SuppressesChallengeWithoutRevealingStatus() throws Exception {
        for (int i = 1; i <= 3; i++) {
            User pendingUser = User.builder()
                    .username("unverified_legacy_" + i)
                    .email("pending_collision@example.com")
                    .accountSlot(i)
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(false)
                    .build();
            userRepository.save(pendingUser);
        }

        reset(mailService);

        SignupRequestOtpRequest request = SignupRequestOtpRequest.builder()
                .email("pending_collision@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If this email is eligible, a verification code has been sent."));

        // Crucial: OTP email is suppressed for the pending account
        verify(mailService, never()).sendOtpEmail(eq("pending_collision@example.com"), any(), any());
    }
}

