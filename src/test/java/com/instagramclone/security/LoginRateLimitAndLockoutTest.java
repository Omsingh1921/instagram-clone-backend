package com.instagramclone.security;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserKnownDevice;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.repository.UserKnownDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginRateLimitAndLockoutTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserKnownDeviceRepository userKnownDeviceRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        if (redisTemplate != null) {
            java.util.Set<String> keys = redisTemplate.keys("rate_limit:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

        testUser = userRepository.save(User.builder()
                .username("abuse_target")
                .email("abuse_target@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .lockoutUntil(null)
                .build());
    }

    @Test
    @DisplayName("Account Rate Limiting - Repeated login attempts for one account across rotating IPs are throttled with 429")
    void accountTargetedRateLimiting_ShouldThrottleAcrossRotatingIps() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("abuse_target")
                .password("WrongPassword123!")
                .build();

        // Send 10 failed login attempts, each with a different client IP to bypass IP-only rate limiting
        for (int i = 1; i <= 10; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100." + ipSuffix);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 11th attempt from yet another new IP must be rejected by account-targeted rate limiting with HTTP 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.99");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many login attempts for this account")));
    }

    @Test
    @DisplayName("Lockout Protection - A rate-limited attempt does not call handleFailedLogin or mutate database lockout state")
    void rateLimitedAttempt_ShouldNotMutateFailedAttemptsOrLockout() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("abuse_target")
                .password("WrongPassword123!")
                .build();

        // Perform 10 failed attempts across different IPs
        for (int i = 1; i <= 10; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100." + ipSuffix);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)));
        }

        User userAfter10 = userRepository.findById(testUser.getId()).orElseThrow();
        int attemptsRecorded = userAfter10.getFailedLoginAttempts();
        LocalDateTime lockoutUntilRecorded = userAfter10.getLockoutUntil();

        // 11th and 12th attempts are rate-limited with HTTP 429
        for (int i = 11; i <= 12; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100." + ipSuffix);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isTooManyRequests());
        }

        // Verify DB values remained unchanged by the rate-limited requests
        User userAfterRateLimit = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userAfterRateLimit.getFailedLoginAttempts()).isEqualTo(attemptsRecorded);
        assertThat(userAfterRateLimit.getLockoutUntil()).isEqualTo(lockoutUntilRecorded);
    }

    @Test
    @DisplayName("Anti-Enumeration - Nonexistent account identifier is rate-limited without disclosing account existence")
    void nonexistentAccount_ShouldBeRateLimitedIdentically() throws Exception {
        LoginRequest ghostRequest = LoginRequest.builder()
                .usernameOrEmail("ghost_unregistered_account")
                .password("RandomPassword123!")
                .build();

        // Send 10 attempts for the nonexistent identifier
        for (int i = 1; i <= 10; i++) {
            final int ipSuffix = i;
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100." + ipSuffix);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ghostRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 11th attempt for the nonexistent identifier returns identical 429 RATE_LIMIT_EXCEEDED
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.99");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ghostRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many login attempts for this account")));
    }

    @Test
    @DisplayName("Device Tracking Consistency - IDeviceDetectionService receives verified client IP matching rate limiting")
    void deviceTracking_ShouldReceiveVerifiedClientIp() throws Exception {
        LoginRequest validLogin = LoginRequest.builder()
                .usernameOrEmail("abuse_target")
                .password("Password123!")
                .build();

        // Scenario 1: Trusted proxy (127.0.0.1) forwards legitimate client (203.0.113.77)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        })
                        .header("X-Forwarded-For", "203.0.113.77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        List<UserKnownDevice> devices1 = userKnownDeviceRepository.findAll();
        assertThat(devices1).isNotEmpty();
        assertThat(devices1.get(0).getLastIpAddress()).isEqualTo("203.0.113.77");

        // Scenario 2: Untrusted direct peer (198.51.100.99) sends spoofed X-Forwarded-For (1.2.3.4)
        userKnownDeviceRepository.deleteAll();
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.99");
                            return req;
                        })
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        List<UserKnownDevice> devices2 = userKnownDeviceRepository.findAll();
        assertThat(devices2).isNotEmpty();
        // The genuine socket address (198.51.100.99) is recorded; spoofed 1.2.3.4 is safely ignored
        assertThat(devices2.get(0).getLastIpAddress()).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("Concurrency & Atomicity - Concurrent login attempts on an account are atomically throttled")
    void concurrentLoginAttempts_ShouldAtomicallyThrottle() throws Exception {
        int threads = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        AtomicInteger unauthorizedCount = new AtomicInteger(0);

        LoginRequest badLogin = LoginRequest.builder()
                .usernameOrEmail("abuse_target")
                .password("WrongPassword123!")
                .build();

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int statusCode = mockMvc.perform(post("/api/v1/auth/login")
                                    .with(req -> {
                                        req.setRemoteAddr("198.51.100." + threadId);
                                        return req;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(badLogin)))
                            .andReturn().getResponse().getStatus();

                    if (statusCode == 429) {
                        rateLimitedCount.incrementAndGet();
                    } else if (statusCode == 401) {
                        unauthorizedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        // Exactly 10 requests allowed by the account limit (status 401), and remaining 5 throttled (status 429)
        assertThat(unauthorizedCount.get()).isEqualTo(10);
        assertThat(rateLimitedCount.get()).isEqualTo(5);
    }
}
