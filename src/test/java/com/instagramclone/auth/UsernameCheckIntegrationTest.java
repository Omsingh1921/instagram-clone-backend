package com.instagramclone.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagramclone.common.constants.ValidationConstants;
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

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsernameCheckIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Pattern USERNAME_PATTERN = Pattern.compile(ValidationConstants.USERNAME_REGEX);

    @BeforeEach
    void setUp() {
        // Register an existing user to test collision handling
        if (!userRepository.existsByUsername("taken_user")) {
            userRepository.save(User.builder()
                    .username("taken_user")
                    .email("taken@example.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .build());
        }
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-username - Available username returns available=true and empty suggestions")
    void checkUsername_WhenAvailable_ShouldReturnAvailableTrue() throws Exception {
        String testUsername = "unique_handle_" + System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", testUsername)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(testUsername.toLowerCase()))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.message").value("Username is available"))
                .andExpect(jsonPath("$.data.suggestions").isArray())
                .andExpect(jsonPath("$.data.suggestions.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-username - Taken username returns available=false and valid alternative suggestions")
    void checkUsername_WhenTaken_ShouldReturnSuggestions() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "taken_user")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("taken_user"))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Username is already taken"))
                .andExpect(jsonPath("$.data.suggestions").isArray())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode suggestions = root.path("data").path("suggestions");

        assertThat(suggestions.size()).isGreaterThanOrEqualTo(4);

        for (JsonNode suggestionNode : suggestions) {
            String suggestion = suggestionNode.asText();
            // 1. Must match standard Instagram username regex
            assertThat(USERNAME_PATTERN.matcher(suggestion).matches())
                    .as("Suggestion '%s' must match username regex", suggestion)
                    .isTrue();

            // 2. Must not be the taken username itself
            assertThat(suggestion).isNotEqualTo("taken_user");

            // 3. Must be genuinely available in database
            assertThat(userRepository.existsByUsername(suggestion))
                    .as("Suggestion '%s' must not exist in DB", suggestion)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-username - Reserved username returns available=false and suggestions")
    void checkUsername_WhenReserved_ShouldReturnReservedMessageAndSuggestions() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "admin")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Username is reserved and cannot be registered"))
                .andExpect(jsonPath("$.data.suggestions").isArray())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode suggestions = root.path("data").path("suggestions");
        assertThat(suggestions.size()).isGreaterThanOrEqualTo(4);

        for (JsonNode suggestionNode : suggestions) {
            String suggestion = suggestionNode.asText();
            assertThat(USERNAME_PATTERN.matcher(suggestion).matches()).isTrue();
            assertThat(suggestion).isNotEqualTo("admin");
        }
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-username - Invalid format (with spaces & symbols) returns suggestions based on sanitized base")
    void checkUsername_WhenInvalidFormat_ShouldSanitizeAndProvideSuggestions() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "spider man! 2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Username format is invalid"))
                .andExpect(jsonPath("$.data.suggestions").isArray())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode suggestions = root.path("data").path("suggestions");
        assertThat(suggestions.size()).isGreaterThanOrEqualTo(1);

        for (JsonNode suggestionNode : suggestions) {
            String suggestion = suggestionNode.asText();
            assertThat(USERNAME_PATTERN.matcher(suggestion).matches()).isTrue();
        }
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-username - Empty or missing username returns available=false")
    void checkUsername_WhenMissingOrBlank_ShouldReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-username")
                        .param("username", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Username is required"))
                .andExpect(jsonPath("$.data.suggestions.length()").value(0));

        mockMvc.perform(get("/api/v1/auth/check-username")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.message").value("Username is required"));
    }
}
