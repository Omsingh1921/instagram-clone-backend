package com.instagramclone.user;


import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.user.dto.request.UpdateProfileRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest extends com.instagramclone.BaseIntegrationTest {

    private String userToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        String signupToken = createValidSignupTicket("peter@bugle.com");
        RegisterRequest register = RegisterRequest.builder()
                .signupToken(signupToken)
                .username("peter_parker")
                .password("Spidey2026!")
                .displayName("Spider-Man")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("peter_parker")
                .password("Spidey2026!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        this.userToken = root.path("data").path("accessToken").asText();
        this.userId = UUID.fromString(root.path("data").path("user").path("id").asText());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - Without JWT should return 401 Unauthorized")
    void getCurrentUser_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - With valid JWT should return authenticated profile")
    void getCurrentUser_WithValidToken_ShouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.username").value("peter_parker"))
                .andExpect(jsonPath("$.data.displayName").value("Spider-Man"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} - Should return public profile for user")
    void getUserProfile_ShouldReturnPublicProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.username").value("peter_parker"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/profile - Should update bio, display name, and website")
    void updateProfile_ShouldUpdateFieldsSuccessfully() throws Exception {
        UpdateProfileRequest update = UpdateProfileRequest.builder()
                .displayName("Friendly Neighborhood Spidey")
                .bio("Your friendly neighborhood photographer and hero")
                .websiteUrl("https://spiderman.marvel.com")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Friendly Neighborhood Spidey"))
                .andExpect(jsonPath("$.data.bio").value("Your friendly neighborhood photographer and hero"))
                .andExpect(jsonPath("$.data.websiteUrl").value("https://spiderman.marvel.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/search - Should return paginated users matching query")
    void searchUsers_ShouldReturnMatchingList() throws Exception {
        mockMvc.perform(get("/api/v1/users/search")
                        .header("Authorization", "Bearer " + userToken)
                        .param("q", "peter")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].username").value("peter_parker"));
    }

    @Test
    @DisplayName("GET /api/v1/users/username/{username} - Should return public profile by username")
    void getUserProfileByUsername_ShouldReturnPublicProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/username/peter_parker")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.username").value("peter_parker"));
    }

    @Test
    @DisplayName("GET /api/v1/users/username/{username} - Nonexistent username should return 404")
    void getUserProfileByUsername_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/users/username/non_existent_user_999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/profile - Should update username successfully")
    void updateProfile_WithNewUsername_ShouldUpdateUsernameSuccessfully() throws Exception {
        UpdateProfileRequest update = UpdateProfileRequest.builder()
                .username("peter_spidey_new")
                .displayName("Spider-Man 2.0")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("peter_spidey_new"))
                .andExpect(jsonPath("$.data.displayName").value("Spider-Man 2.0"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/profile - Duplicate username should return 409 Conflict")
    void updateProfile_WithDuplicateUsername_ShouldReturn409Conflict() throws Exception {
        // Create second user
        String signupToken2 = createValidSignupTicket("miles@spider.com");
        RegisterRequest register2 = RegisterRequest.builder()
                .signupToken(signupToken2)
                .username("miles_morales")
                .password("Spidey2026!")
                .displayName("Miles Morales")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2)))
                .andExpect(status().isCreated());

        // Peter attempts to take miles_morales's username
        UpdateProfileRequest update = UpdateProfileRequest.builder()
                .username("miles_morales")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/profile - Empty string should explicitly clear optional fields to null")
    void updateProfile_WithEmptyStrings_ShouldClearOptionalFields() throws Exception {
        // First populate fields
        UpdateProfileRequest populate = UpdateProfileRequest.builder()
                .displayName("Peter Parker")
                .bio("Web-slinger")
                .websiteUrl("https://spider.man")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(populate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Web-slinger"));

        // Now clear with empty string
        UpdateProfileRequest clear = UpdateProfileRequest.builder()
                .bio("")
                .websiteUrl("")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clear)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").isEmpty())
                .andExpect(jsonPath("$.data.websiteUrl").isEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/avatar - Using media asset owned by another user should return 403 Forbidden")
    void updateAvatar_WithMediaOwnedByAnotherUser_ShouldReturn403Forbidden() throws Exception {
        // Create another user
        String signupToken2 = createValidSignupTicket("gwen@spider.com");
        RegisterRequest register2 = RegisterRequest.builder()
                .signupToken(signupToken2)
                .username("gwen_stacy")
                .password("Spidey2026!")
                .displayName("Ghost-Spider")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2)))
                .andExpect(status().isCreated());

        User gwen = userRepository.findByUsername("gwen_stacy").orElseThrow();

        // Create media asset owned by Gwen
        com.instagramclone.media.entity.Media gwenMedia = com.instagramclone.media.entity.Media.builder()
                .owner(gwen)
                .publicId("gwen_secret_avatar_123")
                .secureUrl("https://res.cloudinary.com/instagramclone/image/upload/v1/gwen_avatar.jpg")
                .resourceType("image")
                .mediaType(com.instagramclone.post.enums.MediaType.IMAGE)
                .build();
        mediaRepository.save(gwenMedia);

        // Peter tries to steal Gwen's media asset as his avatar
        com.instagramclone.user.dto.request.UpdateAvatarRequest stealRequest =
                new com.instagramclone.user.dto.request.UpdateAvatarRequest(gwenMedia.getSecureUrl());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stealRequest)))
                .andExpect(status().isForbidden());
    }
}
