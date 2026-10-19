package com.instagramclone.post;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.engagement.dto.request.CreateCommentRequest;
import com.instagramclone.engagement.service.interfaces.IEngagementService;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EngagementCounterCacheTest extends BaseIntegrationTest {

    @Autowired
    private IEngagementService engagementService;

    @Autowired
    private RedisUtility redisUtility;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private UUID postId;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("creator_eng")
                .email("creator_eng@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("viewer_eng")
                .email("viewer_eng@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());

        CreatePostRequest postRequest = CreatePostRequest.builder()
                .caption("Counter test post")
                .visibility(PostVisibility.PUBLIC)
                .media(List.of(
                        PostMediaRequest.builder()
                                .mediaUrl("https://storage.example.com/media/test_counter.jpg")
                                .mediaType(MediaType.IMAGE)
                                .displayOrder(0)
                                .width(1080)
                                .height(1080)
                                .build()
                ))
                .build();

        String response = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        postId = UUID.fromString(objectMapper.readTree(response).get("data").get("id").asText());
    }

    @Test
    @DisplayName("Engagement: Like count cached in Redis and invalidated upon toggle")
    void testLikeCountCachingAndInvalidation() throws Exception {
        String likeCacheKey = CacheConstants.CACHE_COUNT_POST_LIKES_PREFIX + postId;

        // 1. Initial query populates cache with 0
        long count0 = engagementService.getLikeCount(postId);
        assertThat(count0).isEqualTo(0L);

        Optional<String> cachedValue = redisUtility.get(likeCacheKey);
        assertThat(cachedValue).isPresent().contains("0");

        // 2. User B likes the post
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Cache was evicted by toggleLike()
        // Next service query recalculates from MySQL and repopulates Redis cache
        long count1 = engagementService.getLikeCount(postId);
        assertThat(count1).isEqualTo(1L);

        cachedValue = redisUtility.get(likeCacheKey);
        assertThat(cachedValue).isPresent().contains("1");

        // 3. User B un-likes the post
        mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        long countAfterUnlike = engagementService.getLikeCount(postId);
        assertThat(countAfterUnlike).isEqualTo(0L);
    }

    @Test
    @DisplayName("Engagement: Comment count cached in Redis and invalidated upon add/delete comment")
    void testCommentCountCachingAndInvalidation() throws Exception {
        String commentCacheKey = CacheConstants.CACHE_COUNT_POST_COMMENTS_PREFIX + postId;

        // 1. Initial query populates cache with 0
        long count0 = engagementService.getCommentCount(postId);
        assertThat(count0).isEqualTo(0L);

        Optional<String> cachedValue = redisUtility.get(commentCacheKey);
        assertThat(cachedValue).isPresent().contains("0");

        // 2. User B adds a comment
        CreateCommentRequest commentReq = CreateCommentRequest.builder()
                .text("Nice photo!")
                .build();

        String commentResp = mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID commentId = UUID.fromString(objectMapper.readTree(commentResp).get("data").get("id").asText());

        // After adding comment, cache should be invalidated
        long count1 = engagementService.getCommentCount(postId);
        assertThat(count1).isEqualTo(1L);

        cachedValue = redisUtility.get(commentCacheKey);
        assertThat(cachedValue).isPresent().contains("1");

        // 3. Delete the comment
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        long countAfterDelete = engagementService.getCommentCount(postId);
        assertThat(countAfterDelete).isEqualTo(0L);
    }
}
