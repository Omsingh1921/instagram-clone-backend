package com.instagramclone.common.util;

import com.instagramclone.security.UserPrincipal;
import com.instagramclone.utility.SecurityUtility;

import java.util.Optional;
import java.util.UUID;

/**
 * Legacy SecurityUtils delegating to the centralized SecurityUtility.
 * Preserves backward compatibility across existing services.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return SecurityUtility.getCurrentUserPrincipal();
    }

    public static UUID getCurrentUserId() {
        return SecurityUtility.getCurrentUserId();
    }

    public static Optional<UUID> getCurrentSessionId() {
        return SecurityUtility.getCurrentSessionId();
    }

    public static String getCurrentUsername() {
        return SecurityUtility.getCurrentUsername();
    }

    public static boolean isCurrentUserAdmin() {
        return SecurityUtility.isCurrentUserAdmin();
    }
}
