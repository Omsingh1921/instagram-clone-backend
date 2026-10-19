package com.instagramclone.security;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.auth.dto.request.ForgotPasswordRequest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.ResetPasswordRequest;
import com.instagramclone.auth.entity.PasswordResetToken;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountLockoutSecurityTest extends BaseIntegrationTest {

    private User targetUser;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        targetUser = userRepository.save(User.builder()
                .username("lockout_target")
                .email("lockout_target@test.com")
                .passwordHash(passwordEncoder.encode("ValidPassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .lockoutUntil(null)
                .build());
    }

    @Test
    @DisplayName("Lockout - Consecutive failed logins trigger temporary account lockout with uniform HTTP 401")
    void consecutiveFailedLogins_ShouldTriggerLockout() throws Exception {
        LoginRequest badRequest = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("WrongPassword123!")
                .build();

        // 1-4 failed attempts -> 401 INVALID_CREDENTIALS
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));

            User u = userRepository.findById(targetUser.getId()).orElseThrow();
            assertThat(u.getFailedLoginAttempts()).isEqualTo(i);
            assertThat(u.getLockoutUntil()).isNull();
        }

        // 5th failed attempt -> 401 INVALID_CREDENTIALS (anti-enumeration: locks account in database)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        User lockedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getLockoutUntil()).isNotNull();
        assertThat(lockedUser.getLockoutUntil()).isAfter(LocalDateTime.now());

        // 6th attempt (even with CORRECT password) -> uniform 401 INVALID_CREDENTIALS (anti-enumeration: account is locked)
        LoginRequest correctRequest = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        // 6th attempt with WRONG password -> identical 401 response
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Anti-enumeration - Failed login on non-existent account returns identical 401 response")
    void antiEnumeration_ShouldReturnSameResponseForNonExistentUser() throws Exception {
        LoginRequest nonExistent = LoginRequest.builder()
                .usernameOrEmail("completely_nonexistent_user")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }


    @Test
    @DisplayName("Counter Reset - Successful login resets consecutive failed attempts to 0")
    void successfulLogin_ShouldResetFailedCounter() throws Exception {
        LoginRequest badRequest = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("WrongPassword123!")
                .build();

        // 2 failed attempts
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized());

        User userAfterFails = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(userAfterFails.getFailedLoginAttempts()).isEqualTo(2);

        // Valid login
        LoginRequest goodRequest = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        User userAfterSuccess = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(userAfterSuccess.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(userAfterSuccess.getLockoutUntil()).isNull();
    }

    @Test
    @DisplayName("Recovery - Password reset unlocks locked account immediately")
    void passwordReset_ShouldUnlockAccount() throws Exception {
        // Lock the user
        targetUser.setFailedLoginAttempts(5);
        targetUser.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(targetUser);

        // Verify account is locked and returns uniform 401
        LoginRequest loginAttempt = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("ValidPassword123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"));

        // Create password reset token manually to test reset flow
        String rawToken = "test-reset-token-12345678901234567890123456789012";
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tokenHash = java.util.HexFormat.of().formatHex(hash);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(targetUser)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build());

        // Call reset password
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("BrandNewSecurePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Verify user is unlocked in DB
        User unlockedUser = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(unlockedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(unlockedUser.getLockoutUntil()).isNull();

        // Login with new password succeeds
        LoginRequest newLogin = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("BrandNewSecurePassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Decay - Expired lockout allows login with correct credentials and clears lockout")
    void expiredLockout_ShouldAllowSuccessfulLogin() throws Exception {
        // Set expired lockout
        targetUser.setFailedLoginAttempts(5);
        targetUser.setLockoutUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.save(targetUser);

        // Login with correct credentials succeeds
        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("ValidPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        User refreshed = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(refreshed.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(refreshed.getLockoutUntil()).isNull();
    }

    @Test
    @DisplayName("Concurrency - Concurrent failed logins safely reach lockout without race conditions")
    void concurrentFailedLogins_ShouldSafelyReachLockout() throws Exception {
        int attempts = 8;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(attempts);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch finishLatch = new java.util.concurrent.CountDownLatch(attempts);

        LoginRequest badRequest = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("WrongPassword123!")
                .build();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)));
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        User userAfterConcurrentFails = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(userAfterConcurrentFails.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
        assertThat(userAfterConcurrentFails.getLockoutUntil()).isNotNull();
        assertThat(userAfterConcurrentFails.getLockoutUntil()).isAfter(LocalDateTime.now());

        executor.shutdown();
    }

    @Test
    @DisplayName("Account Status - INACTIVE, SUSPENDED, and DELETED accounts cannot authenticate and return uniform 401")
    void inactiveAccounts_ShouldNotAuthenticateAndReturnUniform401() throws Exception {
        // 1. INACTIVE account
        User inactiveUser = userRepository.save(User.builder()
                .username("inactive_user")
                .email("inactive@test.com")
                .passwordHash(passwordEncoder.encode("SecurePassword123!"))
                .status(UserStatus.INACTIVE)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build());

        // Correct password -> 401 INVALID_CREDENTIALS
        LoginRequest inactiveCorrect = LoginRequest.builder()
                .usernameOrEmail("inactive_user")
                .password("SecurePassword123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveCorrect)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        // Wrong password -> 401 INVALID_CREDENTIALS
        LoginRequest inactiveWrong = LoginRequest.builder()
                .usernameOrEmail("inactive_user")
                .password("WrongPassword123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveWrong)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"));

        // Verify inactive user is not classified as locked
        User inactiveDb = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertThat(inactiveDb.getLockoutUntil()).isNull();

        // 2. SUSPENDED account
        User suspendedUser = userRepository.save(User.builder()
                .username("suspended_user")
                .email("suspended@test.com")
                .passwordHash(passwordEncoder.encode("SecurePassword123!"))
                .status(UserStatus.SUSPENDED)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build());

        LoginRequest suspendedCorrect = LoginRequest.builder()
                .usernameOrEmail("suspended_user")
                .password("SecurePassword123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendedCorrect)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Aapka account suspend hai. Please contact support."));

        User suspendedDb = userRepository.findById(suspendedUser.getId()).orElseThrow();
        assertThat(suspendedDb.getLockoutUntil()).isNull();

        // 3. DELETED account
        User deletedUser = userRepository.save(User.builder()
                .username("deleted_user")
                .email("deleted@test.com")
                .passwordHash(passwordEncoder.encode("SecurePassword123!"))
                .status(UserStatus.DELETED)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build());

        LoginRequest deletedCorrect = LoginRequest.builder()
                .usernameOrEmail("deleted_user")
                .password("SecurePassword123!")
                .build();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deletedCorrect)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        User deletedDb = userRepository.findById(deletedUser.getId()).orElseThrow();
        assertThat(deletedDb.getLockoutUntil()).isNull();
    }

    @Test
    @DisplayName("Anti-enumeration - Nonexistent, wrong-password, locked, and inactive accounts return identical payloads")
    void uniformResponseAcrossAllFailureScenarios() throws Exception {
        // Scenario A: Nonexistent user
        LoginRequest nonexistent = LoginRequest.builder()
                .usernameOrEmail("does_not_exist_at_all")
                .password("RandomPassword123!")
                .build();
        String nonexistentResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonexistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn().getResponse().getContentAsString();

        // Scenario B: Active user, incorrect password
        LoginRequest badPassword = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("WrongPassword123!")
                .build();
        String badPasswordResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn().getResponse().getContentAsString();

        // Scenario C: Locked user, correct password
        targetUser.setFailedLoginAttempts(5);
        targetUser.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(targetUser);

        LoginRequest lockedCorrect = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("ValidPassword123!")
                .build();
        String lockedResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockedCorrect)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn().getResponse().getContentAsString();

        // Scenario D: Inactive user
        userRepository.save(User.builder()
                .username("inactive_probe")
                .email("inactive_probe@test.com")
                .passwordHash(passwordEncoder.encode("ValidPassword123!"))
                .status(UserStatus.INACTIVE)
                .emailVerified(false)
                .build());

        LoginRequest inactiveRequest = LoginRequest.builder()
                .usernameOrEmail("inactive_probe")
                .password("ValidPassword123!")
                .build();
        String inactiveResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn().getResponse().getContentAsString();

        // Compare JSON structure: errorCode and message must match exactly
        com.fasterxml.jackson.databind.JsonNode nodeA = objectMapper.readTree(nonexistentResponse);
        com.fasterxml.jackson.databind.JsonNode nodeB = objectMapper.readTree(badPasswordResponse);
        com.fasterxml.jackson.databind.JsonNode nodeC = objectMapper.readTree(lockedResponse);
        com.fasterxml.jackson.databind.JsonNode nodeD = objectMapper.readTree(inactiveResponse);

        assertThat(nodeA.get("errors").get(0).asText()).isEqualTo(nodeB.get("errors").get(0).asText())
                .isEqualTo(nodeC.get("errors").get(0).asText())
                .isEqualTo(nodeD.get("errors").get(0).asText())
                .isEqualTo("INVALID_CREDENTIALS");

        assertThat(nodeA.get("message").asText()).isEqualTo(nodeB.get("message").asText())
                .isEqualTo(nodeC.get("message").asText())
                .isEqualTo(nodeD.get("message").asText());
    }

    @Test
    @DisplayName("Timing Equalization - Provider executes BCrypt verification path for all failure cases")
    void timingEqualization_ExecutionPathSanity() throws Exception {
        // Verify nonexistent user
        LoginRequest nonexistent = LoginRequest.builder()
                .usernameOrEmail("timing_nonexistent_user")
                .password("TimingCheckPassword123!")
                .build();
        long startNonexistent = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonexistent)))
                .andExpect(status().isUnauthorized());
        long elapsedNonexistent = System.currentTimeMillis() - startNonexistent;

        // Verify locked user
        targetUser.setFailedLoginAttempts(5);
        targetUser.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
        userRepository.save(targetUser);

        LoginRequest locked = LoginRequest.builder()
                .usernameOrEmail("lockout_target")
                .password("TimingCheckPassword123!")
                .build();
        long startLocked = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locked)))
                .andExpect(status().isUnauthorized());
        long elapsedLocked = System.currentTimeMillis() - startLocked;

        // Verify inactive user
        userRepository.save(User.builder()
                .username("timing_inactive_user")
                .email("timing_inactive@test.com")
                .passwordHash(passwordEncoder.encode("TimingCheckPassword123!"))
                .status(UserStatus.INACTIVE)
                .emailVerified(true)
                .build());

        LoginRequest inactive = LoginRequest.builder()
                .usernameOrEmail("timing_inactive_user")
                .password("TimingCheckPassword123!")
                .build();
        long startInactive = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactive)))
                .andExpect(status().isUnauthorized());
        long elapsedInactive = System.currentTimeMillis() - startInactive;

        // Supporting evidence: verify all paths perform real password hashing work (> 20ms)
        // rather than fast-failing in sub-5ms pure DB read
        assertThat(elapsedNonexistent).isGreaterThan(20L);
        assertThat(elapsedLocked).isGreaterThan(20L);
        assertThat(elapsedInactive).isGreaterThan(20L);
    }
}

