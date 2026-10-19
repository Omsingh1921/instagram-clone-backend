package com.instagramclone.follow.util;

import java.util.UUID;

/**
 * Utility methods for follow relationships and follower count formatting.
 */
public final class FollowUtils {

    private FollowUtils() {
        // Prevent instantiation
    }

    /**
     * Checks if a user is attempting to follow themselves.
     */
    public static boolean isSelfFollow(UUID followerId, UUID followingId) {
        return followerId != null && followerId.equals(followingId);
    }

    /**
     * Formats count into human-friendly strings (e.g. 1500 -> "1.5K", 2300000 -> "2.3M").
     */
    public static String formatCount(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        if (count < 1_000_000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.format("%.1fM", count / 1_000_000.0);
    }
}
