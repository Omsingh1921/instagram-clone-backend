package com.instagramclone.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.security.WebSocketAuthInterceptor;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RefreshTokenRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.entity.RefreshToken;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ITokenBlacklistService tokenBlacklistService;

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    private record AuthTokens(String accessToken, String refreshToken, UUID sessionId) {}

    private AuthTokens registerAndLogin(String username, String email, String password, String userAgent) throws Exception {
        String signupToken = createValidSignupTicket(email);
        RegisterRequest registerReq = RegisterRequest.builder()
                .signupToken(signupToken)
                .username(username)
                .password(password)
                .displayName(username.toUpperCase())
                .build();

        var reqBuilder = post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq));

        if (userAgent != null) {
            reqBuilder.header("User-Agent", userAgent);
        }

        MvcResult result = mockMvc.perform(reqBuilder)
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = root.path("data").path("accessToken").asText();
        String refreshToken = root.path("data").path("refreshToken").asText();
        UUID sessionId = jwtTokenProvider.getSessionIdFromToken(accessToken);

        return new AuthTokens(accessToken, refreshToken, sessionId);
    }

    private AuthTokens login(String usernameOrEmail, String password, String userAgent) throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .usernameOrEmail(usernameOrEmail)
                .password(password)
                .build();

        var reqBuilder = post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq));

        if (userAgent != null) {
            reqBuilder.header("User-Agent", userAgent);
        }

        MvcResult result = mockMvc.perform(reqBuilder)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = root.path("data").path("accessToken").asText();
        String refreshToken = root.path("data").path("refreshToken").asText();
        UUID sessionId = jwtTokenProvider.getSessionIdFromToken(accessToken);

        return new AuthTokens(accessToken, refreshToken, sessionId);
    }

    @Test
    @DisplayName("Scenario 1 & 2: Two devices maintain independent sessions & listing returns correct ownership and metadata")
    void twoDevicesMaintainIndependentSessions_AndListActiveSessions() throws Exception {
        // Device A: Chrome on Windows
        AuthTokens deviceA = registerAndLogin("alice_multi", "alice@sessions.com", "Password123!",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Device B: Safari on iPhone
        AuthTokens deviceB = login("alice_multi", "Password123!",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");

        assertThat(deviceA.sessionId()).isNotNull();
        assertThat(deviceB.sessionId()).isNotNull();
        assertThat(deviceA.sessionId()).isNotEqualTo(deviceB.sessionId());

        // Verify Device A calls GET /api/v1/sessions
        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode sessionsData = objectMapper.readTree(sessionsResult.getResponse().getContentAsString()).path("data");
        assertThat(sessionsData.size()).isEqualTo(2);

        boolean foundCurrentSession = false;
        boolean foundOtherSession = false;

        for (JsonNode session : sessionsData) {
            UUID sid = UUID.fromString(session.path("id").asText());
            boolean isCurrent = session.path("currentSession").asBoolean();
            String deviceName = session.path("deviceName").asText();
            String ipAddress = session.path("ipAddress").asText();

            assertThat(ipAddress).contains("***"); // Verified IP masking

            if (sid.equals(deviceA.sessionId())) {
                assertThat(isCurrent).isTrue();
                assertThat(deviceName).contains("Chrome");
                foundCurrentSession = true;
            } else if (sid.equals(deviceB.sessionId())) {
                assertThat(isCurrent).isFalse();
                assertThat(deviceName).contains("Safari");
                foundOtherSession = true;
            }
        }

        assertThat(foundCurrentSession).isTrue();
        assertThat(foundOtherSession).isTrue();
    }

    @Test
    @DisplayName("Scenario 3 & 7: Single-session revocation affects only selected session and immediately invalidates its access token")
    void revokeSingleSession_RevokesTargetOnly_AndInvalidatesItsAccessToken() throws Exception {
        AuthTokens deviceA = registerAndLogin("bob_revoke", "bob@sessions.com", "Password123!", "Chrome on Windows");
        AuthTokens deviceB = login("bob_revoke", "Password123!", "Safari on iPhone");

        // Device A revokes Device B's session
        mockMvc.perform(delete("/api/v1/sessions/" + deviceB.sessionId())
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 1. Device B's access token MUST now be rejected by JwtAuthenticationFilter
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceB.accessToken()))
                .andExpect(status().isUnauthorized());

        // 2. Device B's refresh token MUST be rejected
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(deviceB.refreshToken())
                .build();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // 3. Device A MUST remain completely valid and operational
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("bob_revoke"));
    }

    @Test
    @DisplayName("Scenario 4: Cross-user session revocation is rejected with 404 and does not alter the session")
    void crossUserSessionRevocation_RejectedWith404() throws Exception {
        AuthTokens user1 = registerAndLogin("charlie_1", "charlie1@sessions.com", "Password123!", "Chrome on Windows");
        AuthTokens user2 = registerAndLogin("charlie_2", "charlie2@sessions.com", "Password123!", "Firefox on Linux");

        // User 1 attempts to revoke User 2's session
        mockMvc.perform(delete("/api/v1/sessions/" + user2.sessionId())
                        .header("Authorization", "Bearer " + user1.accessToken()))
                .andExpect(status().isNotFound());

        // Verify User 2's session is untouched and valid
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + user2.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario 5: Single-device logout on Device A leaves Device B active")
    void logoutOnDeviceA_LeavesDeviceBActive() throws Exception {
        AuthTokens deviceA = registerAndLogin("david_multi", "david@sessions.com", "Password123!", "Chrome on Windows");
        AuthTokens deviceB = login("david_multi", "Password123!", "Safari on iPhone");

        // Device A logs out passing its own access token and refresh token
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(deviceA.refreshToken())
                .build();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + deviceA.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // Device A is logged out
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isUnauthorized());

        // CRITICAL CHECK: Device B is STILL authenticated and functional!
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceB.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("david_multi"));

        // Device B can refresh its token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(deviceB.refreshToken())
                .build();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario 6: Global logout-all invalidates all user sessions")
    void logoutAll_InvalidatesAllUserSessions() throws Exception {
        AuthTokens deviceA = registerAndLogin("emma_multi", "emma@sessions.com", "Password123!", "Chrome on Windows");
        AuthTokens deviceB = login("emma_multi", "Password123!", "Safari on iPhone");

        // Device A calls POST /api/v1/auth/logout-all
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isOk());

        // Both Device A and Device B access tokens must now be rejected
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceA.accessToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + deviceB.accessToken()))
                .andExpect(status().isUnauthorized());

        // Both refresh tokens must be rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(deviceA.refreshToken()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(deviceB.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 8: Rotated refresh tokens retain stable session ID")
    void rotatedRefreshToken_RetainsStableSessionId() throws Exception {
        AuthTokens initial = registerAndLogin("frank_rotate", "frank@sessions.com", "Password123!", "Chrome on Windows");
        UUID originalSessionId = initial.sessionId();

        // Perform rotation 1
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initial.refreshToken())
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        String newAccess1 = root1.path("data").path("accessToken").asText();
        String newRefresh1 = root1.path("data").path("refreshToken").asText();
        UUID sid1 = jwtTokenProvider.getSessionIdFromToken(newAccess1);

        assertThat(sid1).isEqualTo(originalSessionId);

        // Perform rotation 2
        RefreshTokenRequest refreshReq2 = RefreshTokenRequest.builder()
                .refreshToken(newRefresh1)
                .build();

        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq2)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        String newAccess2 = root2.path("data").path("accessToken").asText();
        UUID sid2 = jwtTokenProvider.getSessionIdFromToken(newAccess2);

        assertThat(sid2).isEqualTo(originalSessionId);

        // Active session count should still be 1 logical session
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + newAccess2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(originalSessionId.toString()));
    }

    @Test
    @DisplayName("Scenario 9: Revoked refresh token cannot be reused")
    void revokedRefreshToken_CannotBeReused() throws Exception {
        AuthTokens tokens = registerAndLogin("george_reuse", "george@sessions.com", "Password123!", "Chrome on Windows");

        // Rotate once
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(tokens.refreshToken())
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // Attempt to reuse original token MUST fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is expired or revoked"));
    }

    @Test
    @DisplayName("Scenario 10: Concurrent refresh requests cannot both rotate the same token")
    void concurrentRefreshRequests_AtomicProtection() throws Exception {
        AuthTokens tokens = registerAndLogin("helen_race", "helen@sessions.com", "Password123!", "Chrome on Windows");
        String presentedToken = tokens.refreshToken();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    RefreshTokenRequest req = RefreshTokenRequest.builder()
                            .refreshToken(presentedToken)
                            .build();

                    var response = mockMvc.perform(post("/api/v1/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn().getResponse();

                    if (response.getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else if (response.getStatus() == 401) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Exactly one concurrent request must succeed, other must be rejected
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 11: DELETE /api/v1/sessions/other revokes all other sessions")
    void revokeOtherSessions_RevokesAllExceptCurrent() throws Exception {
        AuthTokens dev1 = registerAndLogin("ian_other", "ian@sessions.com", "Password123!", "Device 1");
        AuthTokens dev2 = login("ian_other", "Password123!", "Device 2");
        AuthTokens dev3 = login("ian_other", "Password123!", "Device 3");

        // Device 1 calls DELETE /api/v1/sessions/other
        mockMvc.perform(delete("/api/v1/sessions/other")
                        .header("Authorization", "Bearer " + dev1.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.revokedCount").value(2));

        // Device 1 remains authenticated
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + dev1.accessToken()))
                .andExpect(status().isOk());

        // Devices 2 and 3 access tokens are immediately rejected
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + dev2.accessToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + dev3.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 12: Password change still revokes all user sessions")
    void passwordChange_StillRevokesAllSessions() throws Exception {
        AuthTokens dev1 = registerAndLogin("jane_pw", "jane@sessions.com", "OldPassword123!", "Device 1");
        AuthTokens dev2 = login("jane_pw", "OldPassword123!", "Device 2");

        // Change password from Dev 1
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + dev1.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Both sessions must now be rejected
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + dev1.accessToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + dev2.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 13: Disabled or unverified user tokens remain blocked by security gates")
    void disabledOrUnverifiedAccount_BlockedBySessionSecurityGates() throws Exception {
        User disabledUser = userRepository.save(User.builder()
                .username("disabled_session_user")
                .email("disabled@session.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.SUSPENDED)
                .emailVerified(true)
                .build());

        UUID sid = UUID.randomUUID();
        String disabledToken = jwtTokenProvider.generateTokenForUser(
                disabledUser.getId(), disabledUser.getUsername(), disabledUser.getEmail(), sid);

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + disabledToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 14: WebSocket STOMP CONNECT rejects a revoked session")
    void webSocketStompConnect_RejectsRevokedSession() throws Exception {
        AuthTokens dev = registerAndLogin("ws_session_user", "ws@session.com", "Password123!", "Chrome on Windows");

        // Initially STOMP CONNECT succeeds
        StompHeaderAccessor accessorValid = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessorValid.setNativeHeader("Authorization", "Bearer " + dev.accessToken());
        Message<byte[]> messageValid = MessageBuilder.createMessage(new byte[0], accessorValid.getMessageHeaders());
        Message<?> result = webSocketAuthInterceptor.preSend(messageValid, null);
        assertThat(result).isNotNull();

        // Revoke the session
        tokenBlacklistService.blacklistSession(dev.sessionId(), 900000L);

        // STOMP CONNECT must now throw MessageDeliveryException
        StompHeaderAccessor accessorRevoked = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessorRevoked.setNativeHeader("Authorization", "Bearer " + dev.accessToken());
        Message<byte[]> messageRevoked = MessageBuilder.createMessage(new byte[0], accessorRevoked.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(messageRevoked, null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Session has been revoked");
    }

    @Test
    @DisplayName("Scenario 15: Migration compatibility is retained for existing legacy refresh-token rows")
    void migrationCompatibility_RetainedForExistingRows() throws Exception {
        User user = userRepository.save(User.builder()
                .username("legacy_user")
                .email("legacy@session.com")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        // Legacy record simulated with null metadata fields
        UUID legacySessionId = UUID.randomUUID();
        RefreshToken legacyToken = RefreshToken.builder()
                .user(user)
                .sessionId(legacySessionId)
                .tokenHash("legacy_token_hash_value")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .lastActivityAt(LocalDateTime.now().minusDays(1))
                .deviceName(null)
                .ipAddress(null)
                .userAgent(null)
                .build();
        refreshTokenRepository.save(legacyToken);

        String legacyAccessToken = jwtTokenProvider.generateTokenForUser(
                user.getId(), user.getUsername(), user.getEmail(), legacySessionId);

        MvcResult result = mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + legacyAccessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.size()).isEqualTo(1);
        assertThat(data.get(0).path("id").asText()).isEqualTo(legacySessionId.toString());
        assertThat(data.get(0).path("deviceName").asText()).isEqualTo("Web Browser (Direct Client)");
        assertThat(data.get(0).path("currentSession").asBoolean()).isTrue();
    }
}
