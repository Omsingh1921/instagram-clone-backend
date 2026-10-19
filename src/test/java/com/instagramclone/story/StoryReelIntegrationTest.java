package com.instagramclone.story;


import java.util.UUID;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.story.dto.request.CreateStoryRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StoryReelIntegrationTest extends BaseIntegrationTest {

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user = userRepository.save(User.builder()
                .username("storyteller")
                .email("storyteller@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        token = jwtTokenProvider.generateTokenForUser(user.getId(), user.getUsername(), user.getEmail());
    }

    @Test
    @DisplayName("Story - Create story, view story, and delete story")
    void storyLifecycle_ShouldWorkCorrectly() throws Exception {
        CreateStoryRequest request = CreateStoryRequest.builder()
                .mediaUrl("https://storage.example.com/stories/story1.jpg")
                .mediaType(com.instagramclone.post.enums.MediaType.IMAGE)
                .build();

        String response = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mediaUrl").value("https://storage.example.com/stories/story1.jpg"))
                .andReturn().getResponse().getContentAsString();

        UUID storyId = UUID.fromString(objectMapper.readTree(response).get("data").get("id").asText());

        // Mark viewed
        mockMvc.perform(post("/api/v1/stories/{storyId}/view", storyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Delete story
        mockMvc.perform(delete("/api/v1/stories/{storyId}", storyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(storyRepository.existsById(storyId)).isFalse();
    }

    @Test
    @DisplayName("Reels - Create reel and retrieve reels feed")
    void reelLifecycle_ShouldWorkCorrectly() throws Exception {
        CreateReelRequest request = CreateReelRequest.builder()
                .videoUrl("https://storage.example.com/reels/video1.mp4")
                .caption("My first viral reel")
                .durationMs(15000L)
                .build();

        mockMvc.perform(post("/api/v1/reels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.caption").value("My first viral reel"));

        // Retrieve feed
        mockMvc.perform(get("/api/v1/reels")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].caption").value("My first viral reel"));
    }
}
