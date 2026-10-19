package com.instagramclone.security.constants;

/**
 * Domain constants for security filters, token headers, and account lockout thresholds.
 */
public final class SecurityModuleConstants {

    private SecurityModuleConstants() {
        // Prevent instantiation
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 15;
    public static final int TOKEN_MIN_LENGTH = 10;
}
