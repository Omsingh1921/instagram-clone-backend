package com.instagramclone.notification.common;

import com.instagramclone.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common lightweight representation of a user notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummary {

    private UUID id;
    private UUID recipientId;
    private UUID actorId;
    private String actorUsername;
    private String actorAvatarUrl;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private Instant createdAt;
}
