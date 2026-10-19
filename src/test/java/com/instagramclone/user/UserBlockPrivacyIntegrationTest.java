package com.instagramclone.user;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.request.CreateConversationRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserBlockPrivacyIntegrationTest extends BaseIntegrationTest {

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userA = userRepository.save(User.builder()
                .username("alice_blocker")
                .email("alice@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userB = userRepository.save(User.builder()
                .username("bob_blocked")
                .email("bob@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenA = jwtTokenProvider.generateTokenForUser(userA.getId(), userA.getUsername(), userA.getEmail());
        tokenB = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());
    }

    @Test
    @DisplayName("Block Privacy - Blocked user cannot follow blocker")
    void follow_WhenBlockedByTarget_ShouldBeRejected() throws Exception {
        // Alice blocks Bob
        userBlockRepository.save(UserBlock.builder()
                .blocker(userA)
                .blocked(userB)
                .build());

        // Bob attempts to follow Alice -> rejected
        mockMvc.perform(post("/api/v1/users/" + userA.getId() + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot follow this user"));
    }

    @Test
    @DisplayName("Block Privacy - Blocker cannot follow blocked user without unblocking first")
    void follow_WhenUserBlockedTarget_ShouldBeRejected() throws Exception {
        // Alice blocks Bob
        userBlockRepository.save(UserBlock.builder()
                .blocker(userA)
                .blocked(userB)
                .build());

        // Alice attempts to follow Bob -> rejected
        mockMvc.perform(post("/api/v1/users/" + userB.getId() + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You have blocked this user. Unblock them first to follow."));
    }

    @Test
    @DisplayName("Block Privacy - Blocked user cannot initiate direct conversation with blocker")
    void conversation_WhenBlocked_ShouldBeRejected() throws Exception {
        // Alice blocks Bob
        userBlockRepository.save(UserBlock.builder()
                .blocker(userA)
                .blocked(userB)
                .build());

        CreateConversationRequest req = CreateConversationRequest.builder()
                .recipientId(userA.getId())
                .build();

        // Bob attempts to start a conversation with Alice -> rejected
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot create conversation with this user"));
    }
}
