package com.instagramclone.follow;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FollowIntegrationTest extends BaseIntegrationTest {

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("usera")
                .email("usera@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("userb")
                .email("userb@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());
    }

    @Test
    @DisplayName("Follow - User A follows User B successfully")
    void followUser_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/follow", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.following").value(true))
                .andExpect(jsonPath("$.data.followerCount").value(1));

        assertThat(followRepository.existsByFollowerIdAndFollowingId(userA.getId(), userB.getId())).isTrue();
    }

    @Test
    @DisplayName("Follow - Cannot follow self (400 Bad Request)")
    void followSelf_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/follow", userA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unfollow - User A unfollows User B")
    void unfollowUser_ShouldSucceed() throws Exception {
        // First follow
        mockMvc.perform(post("/api/v1/users/{userId}/follow", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Then unfollow
        mockMvc.perform(delete("/api/v1/users/{userId}/follow", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(followRepository.existsByFollowerIdAndFollowingId(userA.getId(), userB.getId())).isFalse();
    }

    @Test
    @DisplayName("Followers & Following list pagination")
    void getFollowersAndFollowing_ShouldReturnList() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/follow", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Check userB's followers
        mockMvc.perform(get("/api/v1/users/{userId}/followers", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("usera"));

        // Check userA's following
        mockMvc.perform(get("/api/v1/users/{userId}/following", userA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("userb"));
    }
}
