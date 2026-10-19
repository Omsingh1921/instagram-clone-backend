package com.instagramclone.security.util;

import com.instagramclone.security.constants.SecurityModuleConstants;

/**
 * Utility methods for extracting and validating authentication headers and tokens.
 */
public final class SecurityModuleUtils {

    private SecurityModuleUtils() {
        // Prevent instantiation
    }

    /**
     * Extracts Bearer token from raw Authorization HTTP header.
     */
    public static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityModuleConstants.BEARER_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(SecurityModuleConstants.BEARER_PREFIX.length()).trim();
        return token.length() >= SecurityModuleConstants.TOKEN_MIN_LENGTH ? token : null;
    }
}
