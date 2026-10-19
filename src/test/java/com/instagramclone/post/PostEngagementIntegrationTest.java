package com.instagramclone.post;


import java.util.UUID;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.dto.request.UpdatePostRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostEngagementIntegrationTest extends BaseIntegrationTest {

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("creator")
                .email("creator@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("viewer")
                .email("viewer@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());
    }

    @Test
    @DisplayName("Post & Engagement - Create, like, comment, save and IDOR verification")
    void fullPostFlow_ShouldWorkWithOwnershipEnforcement() throws Exception {
        // 1. User A creates a post
        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Hello Instagram World!")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/test1.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .width(1080)
                                .height(1080)
                                .build()
                ))
                .build();

        String createPostResponse = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.caption").value("Hello Instagram World!"))
                .andExpect(jsonPath("$.data.author.username").value("creator"))
                .andReturn().getResponse().getContentAsString();

        UUID postId = UUID.fromString(objectMapper.readTree(createPostResponse).get("data").get("id").asText());

        // 2. User B likes the post
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // 3. User B comments on the post
        CreateCommentRequest commentReq = CreateCommentRequest.builder()
                .text("Awesome photo!")
                .build();

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Awesome photo!"))
                .andExpect(jsonPath("$.data.author.username").value("viewer"));

        // 4. User B saves the post
        mockMvc.perform(post("/api/v1/posts/{postId}/save", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true));

        // 5. User B fetches the post (should see likedByCurrentUser=true, savedByCurrentUser=true)
        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.savedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.commentCount").value(1));

        // 6. IDOR Check: User B attempts to edit User A's post -> MUST return 403 Forbidden
        UpdatePostRequest updateReq = UpdatePostRequest.builder()
                .caption("Hacked Caption")
                .build();

        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // 7. User A updates the post -> Allowed
        UpdatePostRequest legitUpdate = UpdatePostRequest.builder()
                .caption("Updated caption by creator")
                .build();

        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legitUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caption").value("Updated caption by creator"));
    }
}
