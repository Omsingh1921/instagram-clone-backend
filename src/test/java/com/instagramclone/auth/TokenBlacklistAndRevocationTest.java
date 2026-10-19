package com.instagramclone.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RefreshTokenRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TokenBlacklistAndRevocationTest extends BaseIntegrationTest {

    @Autowired
    private ITokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    @DisplayName("ITokenBlacklistService - In-memory and Redis token blacklisting operations")
    void tokenBlacklistService_DirectUnitOperations() {
        String testToken = "test.jwt.token." + UUID.randomUUID();
        assertThat(tokenBlacklistService.isBlacklisted(testToken)).isFalse();

        tokenBlacklistService.blacklistToken(testToken, 10000L);
        assertThat(tokenBlacklistService.isBlacklisted(testToken)).isTrue();

        UUID userId = UUID.randomUUID();
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis() + 5000);

        tokenBlacklistService.blacklistUser(userId, 10000L);
        assertThat(tokenBlacklistService.isUserRevoked(userId, before)).isTrue();
        assertThat(tokenBlacklistService.isUserRevoked(userId, after)).isFalse();
    }

    @Test
    @DisplayName("Logout - Should blacklist access token and reject subsequent API calls")
    void logout_ShouldBlacklistAccessToken() throws Exception {
        String signupToken = createValidSignupTicket("smith@matrix.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("agent_smith")
                .password("Password123!")
                .displayName("Agent Smith")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("agent_smith")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = root.path("data").path("accessToken").asText();
        String refreshToken = root.path("data").path("refreshToken").asText();

        // 1. Verify access token works initially
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("agent_smith"));

        // 2. Perform logout passing Bearer token
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Subsequent request with blacklisted access token MUST be rejected
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Password Change - Should invalidate active user sessions and refresh tokens")
    void changePassword_ShouldInvalidateActiveUserSessions() throws Exception {
        String signupToken = createValidSignupTicket("neo@matrix.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("neo_anderson")
                .password("OldPass123!")
                .displayName("Thomas Anderson")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("neo_anderson")
                .password("OldPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String oldAccessToken = root.path("data").path("accessToken").asText();
        String oldRefreshToken = root.path("data").path("refreshToken").asText();

        // Change password
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewStrongPass123!")
                .confirmPassword("NewStrongPass123!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + oldAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Previous access token must now be rejected
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());

        // Previous refresh token must now be rejected
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(oldRefreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // User can log in with new password
        LoginRequest newLogin = LoginRequest.builder()
                .usernameOrEmail("neo_anderson")
                .password("NewStrongPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }
}
