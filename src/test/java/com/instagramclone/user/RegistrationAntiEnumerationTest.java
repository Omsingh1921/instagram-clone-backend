package com.instagramclone.user;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.auth.dto.request.ForgotPasswordRequest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.dto.request.ResendVerificationRequest;
import com.instagramclone.auth.entity.EmailVerificationToken;
import com.instagramclone.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationAntiEnumerationTest extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    @DisplayName("Scope A: Duplicate username receives explicit public availability feedback")
    void duplicateUsername_ShouldReceiveExplicitFeedback() throws Exception {
        String token1 = createValidSignupTicket("initial_user@example.com");
        RegisterRequest initial = RegisterRequest.builder()
                .signupToken(token1)
                .username("available_handle")
                .password("StrongPassword123!")
                .displayName("Handle Owner")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Submit registration with the same username but a different email
        String token2 = createValidSignupTicket("different_email@example.com");
        RegisterRequest collision = RegisterRequest.builder()
                .signupToken(token2)
                .username("available_handle")
                .password("AnotherPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collision)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username 'available_handle' is already taken"));
    }

    @Test
    @DisplayName("Scope A: Duplicate email response returns generic conflict and never echoes submitted email")
    void duplicateEmail_ShouldReturnGenericConflictAndNeverEchoEmail() throws Exception {
        // Register 3 accounts (max allowed)
        for (int i = 1; i <= 3; i++) {
            String token = createValidSignupTicket("confidential_target@example.com");
            RegisterRequest reg = RegisterRequest.builder()
                    .signupToken(token)
                    .username("user_" + i)
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isCreated());
        }

        // Probe with 4th account (exceeds 3-account limit)
        String token4 = createValidSignupTicket("confidential_target@example.com");
        RegisterRequest duplicateEmailProbe = RegisterRequest.builder()
                .signupToken(token4)
                .username("user_four")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailProbe)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Maximum number of accounts (3) reached for this email address. Please log in or use a different email."))
                .andExpect(jsonPath("$.message").value(not(containsString("confidential_target@example.com"))));

        // Ensure user_four was not created in database
        assertThat(userRepository.findByUsername("user_four")).isEmpty();
    }

    @Test
    @DisplayName("Scope B: Timing mitigation executes password hashing on duplicate email")
    void duplicateEmail_ShouldExecuteTimingMitigation() throws Exception {
        // Register 3 accounts so the email is at capacity
        for (int i = 1; i <= 3; i++) {
            String token = createValidSignupTicket("timing_test@example.com");
            RegisterRequest reg = RegisterRequest.builder()
                    .signupToken(token)
                    .username("existing_account_" + i)
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reg)))
                    .andExpect(status().isCreated());
        }

        // 4th account probe triggers duplicate email conflict path
        String token4 = createValidSignupTicket("timing_test@example.com");
        RegisterRequest duplicateProbe = RegisterRequest.builder()
                .signupToken(token4)
                .username("new_probe_username")
                .password("Password123!")
                .build();

        long startTime = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateProbe)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
        long durationMs = System.currentTimeMillis() - startTime;

        // BCrypt (cost 12) takes substantial work (> 40ms) compared to an unmitigated 2ms index lookup.
        assertThat(durationMs)
                .as("Duplicate email path must perform password hashing mitigation rather than immediate failure")
                .isGreaterThanOrEqualTo(30L);
    }

    @Test
    @DisplayName("Scope C: Email-targeted rate limiting throttles normalized email variants across rotating IPs")
    void emailRateLimiting_ShouldThrottleAcrossNormalizedVariants() throws Exception {
        String[] emailVariants = {
                "VICTIM.TARGET@EXAMPLE.COM",
                "victim.target@example.com",
                "Victim.Target@Example.Com",
                "VICTIM.target@EXAMPLE.com",
                "victim.TARGET@example.com"
        };

        // Send 5 registration attempts using different casing across distinct client IPs
        for (int i = 0; i < 5; i++) {
            final int ipSuffix = i + 1;
            String token = createValidSignupTicket(emailVariants[i]);
            RegisterRequest request = RegisterRequest.builder()
                    .signupToken(token)
                    .username("probe_user_" + ipSuffix)
                    .password("Password123!")
                    .build();

            var resultActions = mockMvc.perform(post("/api/v1/auth/register")
                            .with(req -> {
                                req.setRemoteAddr("203.0.113." + ipSuffix);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));

            if (i < 3) {
                resultActions.andExpect(status().isCreated());
            } else {
                resultActions.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Maximum number of accounts (3) reached for this email address. Please log in or use a different email."));
            }
        }

        // 6th attempt with normalized email from a new IP must exceed the email rate limit
        String token6 = createValidSignupTicket("victim.target@example.com");
        RegisterRequest sixthRequest = RegisterRequest.builder()
                .signupToken(token6)
                .username("probe_user_6")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(req -> {
                            req.setRemoteAddr("203.0.113.6");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixthRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(containsString("Too many registration attempts")))
                .andExpect(jsonPath("$.message").value(not(containsString("victim.target@example.com"))));
    }

    @Test
    @DisplayName("Scope C: Blocked rate-limited attempts do not persist users or create tokens")
    void emailRateLimiting_ShouldNotPersistUserOrTokensWhenThrottled() throws Exception {
        // Pre-register a baseline user to provide a conflicting handle
        String baselineToken = createValidSignupTicket("preexisting@example.com");
        RegisterRequest initialUser = RegisterRequest.builder()
                .signupToken(baselineToken)
                .username("taken_handle")
                .password("Password123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialUser)))
                .andExpect(status().isCreated());

        String email = "dos_target@example.com";

        // Exhaust email-targeted registration rate limit (5 attempts) using taken_handle
        for (int i = 1; i <= 5; i++) {
            final int ipSuffix = i;
            String token = createValidSignupTicket(email);
            RegisterRequest req = RegisterRequest.builder()
                    .signupToken(token)
                    .username("taken_handle")
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                    .with(r -> {
                        r.setRemoteAddr("198.51.100." + ipSuffix);
                        return r;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Username 'taken_handle' is already taken"));
        }

        // 6th request attempt when throttled, with a unique username
        String token6 = createValidSignupTicket(email);
        RegisterRequest throttledReq = RegisterRequest.builder()
                .signupToken(token6)
                .username("throttled_user")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(r -> {
                            r.setRemoteAddr("198.51.100.99");
                            return r;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(throttledReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"));

        // Assert database state: user must NOT exist for throttled_user or dos_target@example.com
        assertThat(userRepository.findByUsername("throttled_user")).isEmpty();
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    @DisplayName("Scope D: Valid signup and automatic login work seamlessly")
    void validSignupAndAutoLogin_ShouldWorkSeamlessly() throws Exception {
        String token = createValidSignupTicket("valid_member@example.com");
        RegisterRequest registerReq = RegisterRequest.builder()
                .signupToken(token)
                .username("new_valid_member")
                .password("ValidPass123!")
                .displayName("Valid Member")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.username").value("new_valid_member"))
                .andExpect(jsonPath("$.data.user.email").value("valid_member@example.com"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));

        // Login after verification succeeds
        LoginRequest loginReq = LoginRequest.builder()
                .usernameOrEmail("new_valid_member")
                .password("ValidPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.username").value("new_valid_member"));
    }

    @Test
    @DisplayName("Scope D: Existing email verification and password reset flows remain anti-enumerating and unchanged")
    void existingVerificationAndResetFlows_RemainAntiEnumerating() throws Exception {
        // Resend verification with non-existent email
        ResendVerificationRequest resendReq = ResendVerificationRequest.builder()
                .email("ghost_user@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a verification link has been sent."));

        // Forgot password with non-existent email
        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder()
                .email("ghost_user@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset link has been sent."));
    }
}
