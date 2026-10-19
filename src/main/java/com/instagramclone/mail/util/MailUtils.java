package com.instagramclone.mail.util;

import java.util.Locale;

/**
 * Utility methods for email verification, address masking, and token signature checks.
 */
public final class MailUtils {

    private MailUtils() {
        // Prevent instantiation
    }

    /**
     * Masks recipient email for log output (e.g. user@domain.com -> u***r@domain.com).
     */
    public static String maskRecipient(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * Sanitizes email address to lowercase and trimmed string.
     */
    public static String cleanEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
