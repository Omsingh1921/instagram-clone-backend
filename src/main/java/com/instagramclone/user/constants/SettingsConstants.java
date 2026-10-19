package com.instagramclone.user.constants;

/**
 * Domain constants for the User Settings & Email Change flow:
 * endpoints, rate limit keys, response messages,
 * and service exception descriptions for SettingsController, ISettingsService, and IEmailChangeService.
 */
public final class SettingsConstants {

    private SettingsConstants() {
        // Prevent instantiation
    }

    // ==================== API ENDPOINT PATHS ====================
    public static final String USERS_BASE_PATH = "/api/v1/users";
    public static final String SETTINGS_BASE_PATH = "/api/v1/users/settings";

    public static final String PATH_ME_EMAIL = "/me/email";
    public static final String PATH_ME_EMAIL_PREFERENCES = "/me/email-preferences";
    public static final String PATH_ME_SETTINGS = "/me/settings";
    public static final String PATH_ME_PASSWORD = "/me/password";
    public static final String PATH_USER_BLOCK = "/{userId}/block";
    public static final String PATH_ME_BLOCKS = "/me/blocks";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_EMAIL_CHANGE = "auth-email-change";
    public static final String RATE_LIMIT_PASSWORD = "auth-password";
    public static final String RATE_LIMIT_CHANGE_PASSWORD = "user-change-password";
    public static final String RATE_LIMIT_INITIATE_EMAIL_CHANGE = "user-initiate-email-change";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_BLOCKED_USERS_RETRIEVED = "Blocked users retrieved successfully";
    public static final String MSG_EMAIL_CHANGE_CANCELLED = "Pending email change request cancelled";
    public static final String MSG_EMAIL_CHANGE_DISPATCHED = "Email change verification dispatched";
    public static final String MSG_EMAIL_CHANGE_INITIATED = "Verification email sent to your new email address. Please confirm the change.";
    public static final String MSG_EMAIL_PREFERENCES_RETRIEVED = "Email preferences retrieved successfully";
    public static final String MSG_EMAIL_PREFERENCES_UPDATED = "Email preferences updated successfully";
    public static final String MSG_PASSWORD_CHANGED_SUCCESS = "Password changed successfully";
    public static final String MSG_SETTINGS_FETCHED_SUCCESS = "Settings fetched successfully";
    public static final String MSG_SETTINGS_UPDATED_SUCCESS = "Settings updated successfully";
    public static final String MSG_USER_BLOCKED_SUCCESS = "User blocked successfully";
    public static final String MSG_USER_UNBLOCKED_SUCCESS = "User unblocked successfully";
    public static final String MSG_EMAIL_CHANGE_VERIFICATION_SENT = "Verification code sent to new email address. Security alert dispatched to current address.";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_CANNOT_BLOCK_SELF = "You cannot block yourself";
    public static final String ERR_CURRENT_PASSWORD_PROVIDED_INCORRECT = "Current password provided is incorrect";
    public static final String ERR_CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    public static final String ERR_NEW_PASSWORD_SAME_AS_CURRENT = "New password cannot be identical to the current password";
    public static final String ERR_PASSWORDS_DO_NOT_MATCH = "New password and confirmation password do not match";
    public static final String ERR_TARGET_USER_NOT_FOUND_ID = "Target user not found with ID: ";
    public static final String ERR_USER_NOT_FOUND_ID = "User not found with ID: ";
    public static final String ERR_CURRENT_USER_NOT_FOUND = "Current user not found";
    public static final String ERR_NEW_EMAIL_SAME_AS_CURRENT = "New email address must be different from current email";
    public static final String ERR_EMAIL_ACCOUNTS_LIMIT_REACHED = "The target email address has reached the maximum allowed limit of 3 accounts.";
    public static final String ERR_INVALID_EMAIL_CHANGE_TOKEN = "Invalid or expired email change token";
    public static final String ERR_EMAIL_CHANGE_REQUEST_EXPIRED_OR_PROCESSED = "This email change request has expired or already been processed";
}
