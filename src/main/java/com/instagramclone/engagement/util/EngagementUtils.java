package com.instagramclone.engagement.util;

import com.instagramclone.engagement.constants.EngagementConstants;

/**
 * Utility methods for engagement processing and metrics.
 */
public final class EngagementUtils {

    private EngagementUtils() {
        // Prevent instantiation
    }

    /**
     * Sanitizes comment text by trimming outer whitespace.
     */
    public static String sanitizeComment(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    /**
     * Validates that comment nesting depth does not exceed the maximum allowed depth.
     */
    public static boolean isValidDepth(int depth) {
        return depth >= 0 && depth <= EngagementConstants.MAX_NESTED_COMMENT_DEPTH;
    }

    /**
     * Calculates engagement rate percentage based on interactions and follower reach.
     */
    public static double calculateEngagementRate(long likes, long comments, long followers) {
        if (followers <= 0) {
            return 0.0;
        }
        return ((likes + comments) * 100.0) / followers;
    }
}
