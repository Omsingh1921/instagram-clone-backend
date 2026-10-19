package com.instagramclone.chat;


import java.util.UUID;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.request.CreateConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatIntegrationTest extends BaseIntegrationTest {

    private User alice;
    private User bob;
    private User charlie;
    private String tokenAlice;
    private String tokenBob;
    private String tokenCharlie;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        charlie = userRepository.save(User.builder()
                .username("charlie")
                .email("charlie@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
        tokenBob = jwtTokenProvider.generateTokenForUser(bob.getId(), bob.getUsername(), bob.getEmail());
        tokenCharlie = jwtTokenProvider.generateTokenForUser(charlie.getId(), charlie.getUsername(), charlie.getEmail());
    }

    @Test
    @DisplayName("Chat - Direct conversation creation, messaging, read receipts, and access isolation")
    void fullChatFlow_ShouldEnforceMembershipRules() throws Exception {
        // 1. Alice initiates direct conversation with Bob
        CreateConversationRequest createReq = CreateConversationRequest.builder()
                .recipientId(bob.getId())
                .build();

        String convResponse = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID convId = UUID.fromString(objectMapper.readTree(convResponse).get("data").get("id").asText());

        // 2. Alice sends a message to Bob
        SendMessageRequest msgReq = SendMessageRequest.builder()
                .content("Hey Bob, how are you?")
                .messageType(MessageType.TEXT)
                .build();

        mockMvc.perform(post("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Hey Bob, how are you?"))
                .andExpect(jsonPath("$.data.sender.username").value("alice"));

        // 3. Bob reads the conversation messages
        mockMvc.perform(get("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Hey Bob, how are you?"));

        // 4. Bob marks conversation as read
        mockMvc.perform(post("/api/v1/conversations/{convId}/read", convId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk());

        // 5. Unauthorized User Check: Charlie is NOT a participant -> 403 Forbidden
        mockMvc.perform(get("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenCharlie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenCharlie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isForbidden());
    }
}
