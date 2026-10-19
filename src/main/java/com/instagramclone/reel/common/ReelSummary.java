package com.instagramclone.reel.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common lightweight representation of a Reel for feeds and explore pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReelSummary {

    private UUID id;
    private UUID creatorId;
    private String creatorUsername;
    private String videoUrl;
    private String thumbnailUrl;
    private int durationSeconds;
    private String audioTitle;
    private long viewsCount;
    private long likesCount;
    private long commentsCount;
    private Instant createdAt;
}
