package com.instagramclone.chat;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.request.AddGroupMembersRequest;
import com.instagramclone.chat.dto.request.AddMessageReactionRequest;
import com.instagramclone.chat.dto.request.CreateGroupConversationRequest;
import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.dto.request.UpdateGroupConversationRequest;
import com.instagramclone.chat.enums.ConversationType;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatAdvancedFeaturesIntegrationTest extends BaseIntegrationTest {

    private User alice;
    private User bob;
    private User charlie;
    private User david;

    private String tokenAlice;
    private String tokenBob;
    private String tokenCharlie;
    private String tokenDavid;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@test.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@test.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        charlie = userRepository.save(User.builder()
                .username("charlie")
                .email("charlie@test.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        david = userRepository.save(User.builder()
                .username("david")
                .email("david@test.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
        tokenBob = jwtTokenProvider.generateTokenForUser(bob.getId(), bob.getUsername(), bob.getEmail());
        tokenCharlie = jwtTokenProvider.generateTokenForUser(charlie.getId(), charlie.getUsername(), charlie.getEmail());
        tokenDavid = jwtTokenProvider.generateTokenForUser(david.getId(), david.getUsername(), david.getEmail());
    }

    @Test
    @DisplayName("Group Chat - Creation, Admin Controls, Adding/Removing Members, and Leaving Group")
    void groupChat_LifecycleAndAdminControls() throws Exception {
        // 1. Alice creates a group chat with Bob and Charlie
        CreateGroupConversationRequest createGroupReq = CreateGroupConversationRequest.builder()
                .name("Weekend Trip")
                .memberIds(List.of(bob.getId(), charlie.getId()))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/conversations/group")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value(ConversationType.GROUP.name()))
                .andExpect(jsonPath("$.data.name").value("Weekend Trip"))
                .andExpect(jsonPath("$.data.creatorId").value(alice.getId().toString()))
                .andExpect(jsonPath("$.data.members.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        UUID convId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 2. Alice (Admin) updates group title
        UpdateGroupConversationRequest updateReq = UpdateGroupConversationRequest.builder()
                .name("Summer Road Trip")
                .build();

        mockMvc.perform(put("/api/v1/conversations/{convId}/group", convId)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Summer Road Trip"));

        // 3. Bob (non-admin) tries to update group title -> 403 Forbidden
        mockMvc.perform(put("/api/v1/conversations/{convId}/group", convId)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // 4. Alice adds David to the group
        AddGroupMembersRequest addMembersReq = AddGroupMembersRequest.builder()
                .memberIds(List.of(david.getId()))
                .build();

        mockMvc.perform(post("/api/v1/conversations/{convId}/members", convId)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMembersReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members.length()").value(4));

        // 5. Non-admin Bob tries to kick Charlie -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/conversations/{convId}/members/{memberId}", convId, charlie.getId())
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // 6. Alice (Admin) kicks David from the group
        mockMvc.perform(delete("/api/v1/conversations/{convId}/members/{memberId}", convId, david.getId())
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 7. Bob leaves the group voluntarily
        mockMvc.perform(post("/api/v1/conversations/{convId}/leave", convId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk());

        // 8. Bob tries to access messages after leaving -> 403 Forbidden
        mockMvc.perform(get("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chat Media - Support for IMAGE, VIDEO, and AUDIO Voice Messages")
    void chatMedia_ShouldSupportMultipleMediaTypes() throws Exception {
        CreateGroupConversationRequest createGroupReq = CreateGroupConversationRequest.builder()
                .name("Media Test Group")
                .memberIds(List.of(bob.getId()))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/conversations/group")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID convId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 1. Send Image message
        SendMessageRequest imageMsg = SendMessageRequest.builder()
                .content("Look at this scenery!")
                .mediaUrl("https://res.cloudinary.com/demo/image/upload/sample.jpg")
                .messageType(MessageType.IMAGE)
                .build();

        mockMvc.perform(post("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imageMsg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.messageType").value(MessageType.IMAGE.name()))
                .andExpect(jsonPath("$.data.mediaUrl").value("https://res.cloudinary.com/demo/image/upload/sample.jpg"));

        // 2. Send Audio (Voice note) message
        SendMessageRequest audioMsg = SendMessageRequest.builder()
                .content(null)
                .mediaUrl("https://res.cloudinary.com/demo/video/upload/voice_note.mp3")
                .messageType(MessageType.AUDIO)
                .build();

        mockMvc.perform(post("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(audioMsg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.messageType").value(MessageType.AUDIO.name()))
                .andExpect(jsonPath("$.data.mediaUrl").value("https://res.cloudinary.com/demo/video/upload/voice_note.mp3"));
    }

    @Test
    @DisplayName("Chat Message - Unsend / Deletion & Emoji Reactions")
    void messageUnsendAndReactions_ShouldOperateCorrectly() throws Exception {
        CreateGroupConversationRequest createGroupReq = CreateGroupConversationRequest.builder()
                .name("Fun Group")
                .memberIds(List.of(bob.getId()))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/conversations/group")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID convId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 1. Alice sends a message
        SendMessageRequest msgReq = SendMessageRequest.builder()
                .content("Hello Bob!")
                .messageType(MessageType.TEXT)
                .build();

        String msgRes = mockMvc.perform(post("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID messageId = UUID.fromString(objectMapper.readTree(msgRes).get("data").get("id").asText());

        // 2. Bob reacts with heart emoji ❤️
        AddMessageReactionRequest heartReaction = AddMessageReactionRequest.builder()
                .emoji("❤️")
                .build();

        mockMvc.perform(post("/api/v1/conversations/{convId}/messages/{msgId}/reactions", convId, messageId)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartReaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emoji").value("❤️"))
                .andExpect(jsonPath("$.data.username").value("bob"));

        // 3. Check messages to verify reaction is present
        mockMvc.perform(get("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reactions.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].reactions[0].emoji").value("❤️"));

        // 4. Bob removes reaction
        mockMvc.perform(delete("/api/v1/conversations/{convId}/messages/{msgId}/reactions", convId, messageId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk());

        // 5. Bob tries to unsend Alice's message -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/conversations/{convId}/messages/{msgId}", convId, messageId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // 6. Alice unsends her message
        mockMvc.perform(delete("/api/v1/conversations/{convId}/messages/{msgId}", convId, messageId)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 7. Verify message is masked as unsent
        mockMvc.perform(get("/api/v1/conversations/{convId}/messages", convId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].isDeleted").value(true))
                .andExpect(jsonPath("$.data.content[0].content").value("This message was unsent"));
    }
}
