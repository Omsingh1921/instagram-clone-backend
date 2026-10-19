package com.instagramclone.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RefreshTokenRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest extends com.instagramclone.BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    @DisplayName("Registration - Should register new user and NEVER expose passwordHash")
    void register_ShouldCreateUserAndNeverLeakPassword() throws Exception {
        String signupToken = createValidSignupTicket("john@example.com");
        RegisterRequest request = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("john_doe")
                .password("Password123!")
                .displayName("John Doe")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("john_doe"))
                .andExpect(jsonPath("$.data.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.user.displayName").value("John Doe"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn();

        // Verify password is encrypted in database
        User savedUser = userRepository.findByUsername("john_doe").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Registration - Should reject duplicate username with 409 Conflict")
    void register_ShouldRejectDuplicateUsername() throws Exception {
        String token1 = createValidSignupTicket("alex@example.com");
        RegisterRequest first = RegisterRequest.builder()
                .signupToken(token1)
                .username("alex_smith")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        String token2 = createValidSignupTicket("alex_other@example.com");
        RegisterRequest duplicate = RegisterRequest.builder()
                .signupToken(token2)
                .username("alex_smith")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username 'alex_smith' is already taken"));
    }

    @Test
    @DisplayName("Registration - Should reject duplicate email with 409 Conflict without leaking account status or email")
    void register_ShouldRejectDuplicateEmail() throws Exception {
        // Register 3 accounts for sarah@example.com (the maximum allowed)
        for (int i = 1; i <= 3; i++) {
            String token = createValidSignupTicket("sarah@example.com");
            RegisterRequest req = RegisterRequest.builder()
                    .signupToken(token)
                    .username("sarah" + i)
                    .password("Password123!")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // 4th account with duplicate email must be rejected with 409 Conflict
        String token4 = createValidSignupTicket("sarah@example.com");
        RegisterRequest fourth = RegisterRequest.builder()
                .signupToken(token4)
                .username("sarah4")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fourth)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Maximum number of accounts (3) reached for this email address. Please log in or use a different email."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sarah@example.com"))));
    }

    @Test
    @DisplayName("Login - Should authenticate valid credentials and issue JWT tokens")
    void login_ShouldReturnAuthTokensOnSuccess() throws Exception {
        String token = createValidSignupTicket("bruce@wayne.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(token)
                .username("bruce_wayne")
                .password("GothamHero123!")
                .displayName("Batman")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("bruce_wayne")
                .password("GothamHero123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("bruce_wayne"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Login - Should reject invalid password with 401 Unauthorized")
    void login_ShouldFailWithBadCredentials() throws Exception {
        String token = createValidSignupTicket("clark@dailyplanet.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(token)
                .username("clark_kent")
                .password("Krypton999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("clark_kent")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Token Refresh - Should rotate refresh token and issue new token pair")
    void refresh_ShouldRotateRefreshToken() throws Exception {
        String token = createValidSignupTicket("diana@themyscira.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(token)
                .username("diana_prince")
                .password("WonderWoman123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("diana_prince")
                .password("WonderWoman123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialRefreshToken = root.path("data").path("refreshToken").asText();

        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initialRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode refreshRoot = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshRoot.path("data").path("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

        // Previous token must now be rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }
}
