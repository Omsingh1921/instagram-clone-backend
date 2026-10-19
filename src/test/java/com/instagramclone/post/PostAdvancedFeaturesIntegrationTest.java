package com.instagramclone.post;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.dto.request.PostMediaTagRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.repository.ProfileRepository;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostAdvancedFeaturesIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("creator_adv")
                .email("creator_adv@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        profileRepository.save(Profile.builder()
                .user(userA)
                .displayName("Creator Adv")
                .avatarUrl("https://storage.example.com/avatars/creator.jpg")
                .build());

        userB = userRepository.save(User.builder()
                .username("viewer_adv")
                .email("viewer_adv@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        profileRepository.save(Profile.builder()
                .user(userB)
                .displayName("Viewer Adv")
                .avatarUrl("https://storage.example.com/avatars/viewer.jpg")
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());
    }

    @Test
    @DisplayName("Feature: Get Post Likes returns list of users who liked the post")
    void testGetPostLikes_ShouldReturnUsersList() throws Exception {
        // 1. User A creates a post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Public post for likes list")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/likes_test.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .width(1080)
                                .height(1080)
                                .build()
                ))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID postId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 2. User B likes the post
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));

        // 3. User A or B calls GET /api/v1/posts/{postId}/likes
        mockMvc.perform(get("/api/v1/posts/{postId}/likes", postId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("viewer_adv"))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Viewer Adv"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Security Gap Fixed: Private account post cannot be liked, commented or saved by non-follower")
    void testPrivateAccountPost_CannotBeLikedOrCommentedByNonFollower() throws Exception {
        // 1. Make User A private
        userSettingsRepository.save(UserSettings.builder()
                .user(userA)
                .isPrivate(true)
                .build());

        // 2. User A creates a post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Private secret post")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/private.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .build()
                ))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID postId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 3. User B (non-follower) attempts to like -> 403 Forbidden
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 4. User B attempts to comment -> 403 Forbidden
        CreateCommentRequest commentReq = CreateCommentRequest.builder()
                .text("Trying to comment on private post")
                .build();

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isForbidden());

        // 5. User B attempts to save -> 403 Forbidden
        mockMvc.perform(post("/api/v1/posts/{postId}/save", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security Gap Fixed: Blocked user cannot engage with post (returns 404 Not Found)")
    void testBlockedUser_CannotEngageWithPost() throws Exception {
        // 1. User A blocks User B
        userBlockRepository.save(UserBlock.builder()
                .blocker(userA)
                .blocked(userB)
                .build());

        // 2. User A creates a post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Post before blocked interaction")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/blocked_test.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .build()
                ))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID postId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 3. User B attempts to like -> 404 Not Found (blocked privacy protection)
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Feature: User Tagging on media and fetching tagged posts")
    void testUserTagging_AndFetchTaggedPosts() throws Exception {
        // 1. User A creates post tagging User B
        PostMediaTagRequest tagRequest = PostMediaTagRequest.builder()
                .taggedUserId(userB.getId())
                .xPercent(35.5)
                .yPercent(72.0)
                .build();

        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Awesome beach day with @viewer_adv!")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/tagged_beach.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .tags(List.of(tagRequest))
                                .build()
                ))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.media[0].tags[0].taggedUsername").value("viewer_adv"))
                .andExpect(jsonPath("$.data.media[0].tags[0].xPercent").value(35.5))
                .andExpect(jsonPath("$.data.media[0].tags[0].yPercent").value(72.0))
                .andReturn().getResponse().getContentAsString();

        UUID postId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 2. Fetch User B's tagged posts tab
        mockMvc.perform(get("/api/v1/users/{userId}/posts/tagged", userB.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(postId.toString()))
                .andExpect(jsonPath("$.data.content[0].author.username").value("creator_adv"))
                .andExpect(jsonPath("$.data.content[0].media[0].tags[0].taggedUsername").value("viewer_adv"));
    }
}
