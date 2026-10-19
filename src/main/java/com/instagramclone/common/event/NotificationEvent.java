package com.instagramclone.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private UUID notificationId;
    private UUID recipientId;
    private UUID actorId;
    private String type;
    private UUID postId;
    private UUID commentId;
}
