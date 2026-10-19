package com.instagramclone.feed.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility methods for feed ranking scores, recency decay, and feed item interleaving.
 */
public final class FeedUtils {

    private FeedUtils() {
        // Prevent instantiation
    }

    /**
     * Calculates an algorithmic ranking score for a post based on recency and engagement.
     */
    public static double calculatePostScore(Instant createdAt, long likes, long comments) {
        if (createdAt == null) {
            return 0.0;
        }
        long hoursOld = Math.max(1, Duration.between(createdAt, Instant.now()).toHours());
        double engagement = (likes * 1.0) + (comments * 2.0);
        // Exponential time-decay score
        return (engagement + 1.0) / Math.pow(hoursOld + 2.0, 1.5);
    }
}
