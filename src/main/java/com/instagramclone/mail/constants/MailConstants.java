package com.instagramclone.mail.constants;

/**
 * Domain constants for email templates, dispatch, and unsubscribe tokens.
 */
public final class MailConstants {

    private MailConstants() {
        // Prevent instantiation
    }

    public static final String DEFAULT_FROM_NAME = "Instagram Clone";
    public static final String DEFAULT_FROM_EMAIL = "no-reply@instagramclone.com";
    public static final int UNSUBSCRIBE_TOKEN_EXPIRY_DAYS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int DISPATCH_TIMEOUT_SECONDS = 10;

    // ==================== ERROR MESSAGES ====================
    public static final String ERR_CANNOT_UNSUBSCRIBE_MANDATORY = "Cannot unsubscribe from mandatory security or account notifications";
    public static final String ERR_UNSUBSCRIBE_LINK_EXPIRED = "Unsubscribe link has expired";
    public static final String ERR_INVALID_UNSUBSCRIBE_SIGNATURE = "Invalid unsubscribe signature";
    public static final String ERR_HMAC_SIGNATURE_FAILED = "Failed computing HMAC signature for unsubscribe token";
}
