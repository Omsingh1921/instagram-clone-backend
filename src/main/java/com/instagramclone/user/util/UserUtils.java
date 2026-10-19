package com.instagramclone.user.util;

import com.instagramclone.user.constants.UserConstants;

import java.util.Locale;

/**
 * Domain utility methods for User entities and authentication workflows.
 */
public final class UserUtils {

    private UserUtils() {
        // Prevent instantiation
    }

    /**
     * Masks an email address for privacy and security logs (e.g., peter.parker@example.com -> p***r@example.com).
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domain;
        }
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
    }

    /**
     * Normalizes a username by trimming and converting to lowercase.
     */
    public static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes an email address by trimming and converting to lowercase.
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Formats the user display name, falling back to username if display name is null or blank.
     */
    public static String resolveDisplayName(String displayName, String username) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        return username != null ? username.trim() : "";
    }

    /**
     * Validates whether an account slot number is within allowed bounds (1 to 3).
     */
    public static boolean isValidAccountSlot(Integer slot) {
        return slot != null && slot >= UserConstants.MIN_ACCOUNT_SLOT && slot <= UserConstants.MAX_ACCOUNT_SLOT;
    }
}
