package com.instagramclone.ratelimit.util;

import com.instagramclone.ratelimit.constants.RateLimitConstants;

/**
 * Utility methods for building rate limit keys and calculating retry-after durations.
 */
public final class RateLimitUtils {

    private RateLimitUtils() {
        // Prevent instantiation
    }

    /**
     * Builds a Redis key for a rate-limited endpoint and target identifier (IP or username).
     */
    public static String buildKey(String endpoint, String identifier) {
        String ep = (endpoint != null) ? endpoint.trim() : "default";
        String id = (identifier != null) ? identifier.trim() : "anonymous";
        return RateLimitConstants.RATE_LIMIT_PREFIX + ep + ":" + id;
    }

    /**
     * Calculates the retry-after remaining seconds.
     */
    public static long calculateRetryAfter(long windowSeconds, long elapsedSeconds) {
        return Math.max(1, windowSeconds - elapsedSeconds);
    }
}
