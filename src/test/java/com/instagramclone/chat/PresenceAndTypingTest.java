package com.instagramclone.chat;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.response.PresenceResponse;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PresenceAndTypingTest extends BaseIntegrationTest {

    @Autowired
    private IPresenceService presenceService;

    private User alice;
    private User bob;
    private String tokenAlice;
    private String tokenBob;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        alice = userRepository.save(User.builder()
                .username("alice_presence")
                .email("alice_p@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob_presence")
                .email("bob_p@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
        tokenBob = jwtTokenProvider.generateTokenForUser(bob.getId(), bob.getUsername(), bob.getEmail());
    }

    @Test
    @DisplayName("Presence: Multi-device connection and disconnection semantics")
    void testMultiDevicePresence() {
        UUID userId = alice.getId();
        String sessionDesktop = "session-desktop-" + UUID.randomUUID();
        String sessionMobile = "session-mobile-" + UUID.randomUUID();

        // 1. Initial state: offline
        PresenceResponse initial = presenceService.getPresence(userId);
        assertThat(initial.isOnline()).isFalse();

        // 2. Connect desktop session -> online
        presenceService.userConnected(userId, sessionDesktop);
        PresenceResponse afterDesktop = presenceService.getPresence(userId);
        assertThat(afterDesktop.isOnline()).isTrue();

        // 3. Connect mobile session -> still online
        presenceService.userConnected(userId, sessionMobile);
        PresenceResponse afterMobile = presenceService.getPresence(userId);
        assertThat(afterMobile.isOnline()).isTrue();

        // 4. Disconnect desktop session -> user remains ONLINE because mobile is active
        presenceService.userDisconnected(userId, sessionDesktop);
        PresenceResponse afterDesktopDisconnect = presenceService.getPresence(userId);
        assertThat(afterDesktopDisconnect.isOnline()).isTrue();

        // 5. Disconnect mobile session -> user becomes OFFLINE with recorded lastSeen
        presenceService.userDisconnected(userId, sessionMobile);
        PresenceResponse afterAllDisconnect = presenceService.getPresence(userId);
        assertThat(afterAllDisconnect.isOnline()).isFalse();
        assertThat(afterAllDisconnect.getLastSeen()).isNotNull();
    }

    @Test
    @DisplayName("Typing: Ephemeral typing indicator toggles and expires")
    void testTypingIndicator() {
        UUID convId = UUID.randomUUID();
        UUID userId = alice.getId();

        assertThat(presenceService.isTyping(convId, userId)).isFalse();

        // Set typing to true
        presenceService.setTyping(convId, userId, true);
        assertThat(presenceService.isTyping(convId, userId)).isTrue();

        // Clear typing explicitly
        presenceService.setTyping(convId, userId, false);
        assertThat(presenceService.isTyping(convId, userId)).isFalse();
    }

    @Test
    @DisplayName("Presence API: Enforces privacy and hides status when blocked")
    void testPresencePrivacyEnforcement() throws Exception {
        UUID aliceId = alice.getId();
        UUID bobId = bob.getId();
        String sessionBob = "session-bob-" + UUID.randomUUID();

        // Bob connects
        presenceService.userConnected(bobId, sessionBob);

        // Alice views Bob's presence -> visible
        mockMvc.perform(get("/api/v1/users/" + bobId + "/presence")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.online").value(true));

        // Bob blocks Alice
        userBlockRepository.save(UserBlock.builder()
                .blocker(bob)
                .blocked(alice)
                .build());

        // Alice checks Bob's presence -> masked (online=false, lastSeen=null)
        mockMvc.perform(get("/api/v1/users/" + bobId + "/presence")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(false))
                .andExpect(jsonPath("$.data.lastSeen").doesNotExist());

        // Cleanup
        presenceService.userDisconnected(bobId, sessionBob);
    }
}
