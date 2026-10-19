package com.instagramclone.ratelimit.constants;

/**
 * Domain constants for sliding window rate limiters and redis key prefixes.
 */
public final class RateLimitConstants {

    private RateLimitConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_WINDOW_SECONDS = 60;
    public static final int DEFAULT_MAX_REQUESTS = 100;
    public static final int AUTH_WINDOW_SECONDS = 60;
    public static final int AUTH_MAX_ATTEMPTS = 10;

    public static final String RATE_LIMIT_PREFIX = "ratelimit:";
}
