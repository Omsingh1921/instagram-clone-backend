package com.instagramclone.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.auth.dto.request.SignupRequestOtpRequest;
import com.instagramclone.auth.dto.request.SignupVerifyOtpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ControllerValidationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Endpoint Validation - Login with blank payload returns 400 with standard ApiResponse envelope")
    void login_BlankPayload_Returns400WithSafeFieldErrors() throws Exception {
        LoginRequest badRequest = LoginRequest.builder()
                .usernameOrEmail("")
                .password("")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        JsonNode errors = root.path("errors");
        assertThat(errors.toString()).contains("usernameOrEmail");
        assertThat(errors.toString()).contains("password");
        // Ensure no raw secret or password values are leaked in errors
        assertThat(errors.toString()).doesNotContain("null");
    }

    @Test
    @DisplayName("Endpoint Validation - Signup OTP request with invalid email returns 400")
    void signupOtp_InvalidEmail_Returns400() throws Exception {
        SignupRequestOtpRequest badRequest = SignupRequestOtpRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Endpoint Validation - Signup verify OTP with non-6-digit code returns 400 without leaking OTP")
    void signupVerifyOtp_InvalidOtp_Returns400WithoutLeakingSecret() throws Exception {
        String testSecret = "999";
        SignupVerifyOtpRequest badRequest = SignupVerifyOtpRequest.builder()
                .email("valid.user@example.com")
                .code(testSecret)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // The submitted OTP value must NOT appear in the validation error response
        assertThat(body).doesNotContain("\"" + testSecret + "\"");
    }

    @Test
    @DisplayName("Endpoint Validation - Register without signupToken returns 400")
    void register_MissingSignupToken_Returns400() throws Exception {
        RegisterRequest badRequest = RegisterRequest.builder()
                .username("new_valid_user")
                .password("Password123!")
                .email("new@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Signup verification token is required"));
    }

    @Test
    @DisplayName("Endpoint Validation - Register with oversized signupToken returns 400 Validation failed")
    void register_OversizedSignupToken_Returns400() throws Exception {
        RegisterRequest badRequest = RegisterRequest.builder()
                .signupToken("a".repeat(256))
                .username("new_valid_user")
                .password("Password123!")
                .email("new@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("signupToken: Signup token cannot exceed 255 characters"));
    }

    @Test
    @DisplayName("Endpoint Validation - Register with invalid username pattern returns 400")
    void register_InvalidUsernamePattern_Returns400() throws Exception {
        RegisterRequest badRequest = RegisterRequest.builder()
                .signupToken("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .username(".leadingdot.")
                .password("Password123!")
                .email("valid@example.com")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Endpoint Validation - Malformed JSON body returns 400 BAD_REQUEST")
    void malformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request body"))
                .andExpect(jsonPath("$.errors[0]").value("MALFORMED_REQUEST"));
    }
}
