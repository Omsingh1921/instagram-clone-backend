package com.instagramclone.common;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlingTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Exception Handling - Malformed JSON body should return 400 Bad Request")
    void malformedJson_ShouldReturn400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"badJson\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Exception Handling - Malformed UUID in path variable should return 400 Bad Request")
    void malformedUuid_ShouldReturn400BadRequest() throws Exception {
        User user = userRepository.saveAndFlush(User.builder()
                .username("err_user")
                .email("err@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        String token = jwtTokenProvider.generateTokenForUser(user.getId(), user.getUsername(), user.getEmail());

        mockMvc.perform(get("/api/v1/posts/invalid-uuid-format")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("Observability - Request should receive and echo X-Correlation-ID header")
    void correlationId_ShouldBePresentInResponse() throws Exception {
        // 1. With client-supplied Correlation ID
        mockMvc.perform(get("/api/v1/health")
                        .header("X-Correlation-ID", "custom-trace-id-12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "custom-trace-id-12345"));

        // 2. Without Correlation ID (auto-generated)
        var result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        String generatedId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(generatedId).isNotBlank();
    }
}
