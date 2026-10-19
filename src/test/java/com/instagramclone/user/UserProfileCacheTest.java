package com.instagramclone.user;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.user.dto.request.UpdateProfileRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserProfileCacheTest extends BaseIntegrationTest {

    @Autowired
    private RedisUtility redisUtility;

    private User userA;
    private User userB;
    private User userC;
    private String tokenA;
    private String tokenB;
    private String tokenC;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("star_user")
                .email("star@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("viewer_b")
                .email("b@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userC = userRepository.save(User.builder()
                .username("viewer_c")
                .email("c@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());
        tokenC = jwtTokenProvider.generateTokenForUser(userC.getId(), userC.getUsername(), userC.getEmail());
    }

    @Test
    @DisplayName("User Profile Cache: Caches public profile, evicts on update, and protects privacy boundary")
    void testProfileCachingAndPrivacyBoundary() throws Exception {
        UUID userAId = userA.getId();
        String profileCacheKey = CacheConstants.CACHE_USER_PROFILE_PREFIX + userAId;

        // 1. First fetch -> cache miss, populates Redis cache
        mockMvc.perform(get("/api/v1/users/{userId}", userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("star_user"))
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(false));

        Optional<String> cached = redisUtility.get(profileCacheKey);
        assertThat(cached).isPresent();

        // 2. User A updates bio -> profile cache is evicted
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .bio("Updated celebrity bio!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Cache must have been evicted by updateProfile
        cached = redisUtility.get(profileCacheKey);
        assertThat(cached).isEmpty();

        // 3. User B views again -> cache repopulated with updated bio
        mockMvc.perform(get("/api/v1/users/{userId}", userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Updated celebrity bio!"))
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(false));

        // 4. Privacy boundary check: User B follows User A
        mockMvc.perform(post("/api/v1/users/{userId}/follow", userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Now User B views User A: isFollowed is true
        mockMvc.perform(get("/api/v1/users/{userId}", userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(true));

        // Meanwhile, User C views User A: isFollowed MUST be false (no relationship pollution across viewers)
        mockMvc.perform(get("/api/v1/users/{userId}", userAId)
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followedByCurrentUser").value(false));
    }
}
