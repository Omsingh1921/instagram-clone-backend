package com.instagramclone.story.util;

import com.instagramclone.story.constants.StoryConstants;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility methods for story expiration calculation and ephemeral state checks.
 */
public final class StoryUtils {

    private StoryUtils() {
        // Prevent instantiation
    }

    /**
     * Checks if a story is still active (less than 24 hours old).
     */
    public static boolean isStoryActive(Instant createdAt) {
        if (createdAt == null) {
            return false;
        }
        Instant expiresAt = createdAt.plus(Duration.ofHours(StoryConstants.STORY_LIFETIME_HOURS));
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Calculates the expiration instant for a story created at the specified time.
     */
    public static Instant calculateExpiryTime(Instant createdAt) {
        if (createdAt == null) {
            return Instant.now().plus(Duration.ofHours(StoryConstants.STORY_LIFETIME_HOURS));
        }
        return createdAt.plus(Duration.ofHours(StoryConstants.STORY_LIFETIME_HOURS));
    }

    /**
     * Calculates the remaining seconds of story visibility.
     */
    public static long calculateRemainingSeconds(Instant createdAt) {
        if (createdAt == null) {
            return 0;
        }
        Instant expiresAt = calculateExpiryTime(createdAt);
        long remaining = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(0, remaining);
    }
}
