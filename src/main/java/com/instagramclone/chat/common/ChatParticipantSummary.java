package com.instagramclone.chat.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common representation of a participant in a chat conversation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantSummary {

    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isOnline;
    private Instant lastSeenAt;
}
