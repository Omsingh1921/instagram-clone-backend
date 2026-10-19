package com.instagramclone.story.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common lightweight representation of a Story for tray items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorySummary {

    private UUID id;
    private UUID authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private String mediaUrl;
    private String mediaType;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean isViewed;
}
