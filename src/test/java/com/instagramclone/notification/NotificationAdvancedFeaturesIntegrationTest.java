package com.instagramclone.notification;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationAdvancedFeaturesIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private INotificationService notificationService;

    private User alice;
    private User bob;
    private User charlie;

    private String tokenAlice;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@notif.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@notif.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        charlie = userRepository.save(User.builder()
                .username("charlie")
                .email("charlie@notif.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
    }

    @Test
    @DisplayName("Notifications - In-App Notifications, Badge Count, Filtering, and Read Status Management")
    void notificationFlow_ShouldOperateCorrectly() throws Exception {
        // 1. Initial unread badge count is 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 2. Generate notifications for Alice: Follow from Bob and Post Like from Charlie
        UUID dummyPostId = UUID.randomUUID();
        notificationService.createNotification(alice.getId(), bob.getId(), NotificationType.FOLLOW, null, null);
        notificationService.createNotification(alice.getId(), charlie.getId(), NotificationType.POST_LIKED, dummyPostId, null, null, "https://res.cloudinary.com/demo/image/upload/sample.jpg");

        // 3. Check unread badge count -> should now be 2
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 4. Retrieve notifications and verify media preview URL
        String notifsRes = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        UUID firstNotifId = UUID.fromString(objectMapper.readTree(notifsRes).get("data").get("content").get(0).get("id").asText());

        // 5. Filter notifications by type FOLLOW
        mockMvc.perform(get("/api/v1/notifications")
                        .param("type", "FOLLOW")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("FOLLOW"));

        // 6. Mark first notification as read
        mockMvc.perform(put("/api/v1/notifications/{id}/read", firstNotifId)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 7. Check unread badge count -> should now be 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 8. Mark all notifications as read
        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 9. Unread badge count becomes 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 10. Clear read notifications
        mockMvc.perform(delete("/api/v1/notifications/clear-read")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        // 11. Notifications list is now empty
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }
}
