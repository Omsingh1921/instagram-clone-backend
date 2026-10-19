package com.instagramclone.notification.dto.response;

import com.instagramclone.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private NotificationActorResponse actor;
    private NotificationType type;
    private UUID postId;
    private UUID commentId;
    private UUID reelId;
    private String mediaPreviewUrl;
    private boolean read;
    private LocalDateTime createdAt;
}
