package com.instagramclone.user.constants;

/**
 * Domain constants for the User Profile flow:
 * endpoints, configuration bounds, response messages,
 * and service exception descriptions for UserController and IUserService.
 */
public final class UserConstants {

    private UserConstants() {
        // Prevent instantiation
    }

    // ==================== CONFIGURATION BOUNDS ====================
    public static final int MAX_ACCOUNTS_PER_EMAIL = 3;
    public static final int MIN_ACCOUNT_SLOT = 1;
    public static final int MAX_ACCOUNT_SLOT = 3;

    public static final String DEFAULT_AVATAR_URL = "/default-avatar.png";
    public static final int MAX_BIO_LENGTH = 150;
    public static final int MAX_WEBSITE_LENGTH = 200;
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int ACCOUNT_DELETION_GRACE_PERIOD_DAYS = 15;

    public static final String MSG_ACCOUNT_DELETED_SUCCESS = "Account deletion scheduled. You have 15 days to log in to cancel deletion and reactivate your account.";
    public static final String MSG_ACCOUNT_REACTIVATED = "Welcome back! Your account has been reactivated.";
    public static final String ERR_ACCOUNT_PERMANENTLY_DELETED = "This account has been permanently deleted. The 15-day recovery window has expired.";
    public static final String ERR_ACCOUNT_SUSPENDED = "Aapka account suspend hai. Please contact support.";

    // ==================== API ENDPOINT PATHS ====================
    public static final String USERS_BASE_PATH = "/api/v1/users";
    public static final String PATH_ME = "/me";
    public static final String PATH_USER_BY_ID = "/{userId}";
    public static final String PATH_USER_BY_USERNAME = "/username/{username}";
    public static final String PATH_ME_PROFILE = "/me/profile";
    public static final String PATH_ME_AVATAR = "/me/avatar";
    public static final String PATH_SEARCH = "/search";

    // ==================== SWAGGER & OPENAPI TAGS ====================
    public static final String TAG_USER_PROFILE = "User & Profile";
    public static final String TAG_USER_PROFILE_DESCRIPTION = "Endpoints for viewing and updating user profiles and searching users";

    // ==================== SORT PROPERTIES & FIELDS ====================
    public static final String SORT_BY_USERNAME = "username";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_PROFILE_UPDATED_SUCCESS = "Profile updated successfully";
    public static final String MSG_AVATAR_UPDATED_SUCCESS = "Avatar updated successfully";
    public static final String MSG_USER_FETCHED_SUCCESS = "User fetched successfully";
    public static final String MSG_USERS_FETCHED_SUCCESS = "Users fetched successfully";
    public static final String MSG_INVALID_BIO = "Bio cannot exceed 150 characters";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_USER_NOT_FOUND = "User not found";
    public static final String ERR_USER_NOT_FOUND_ID = "User not found with ID: ";
    public static final String ERR_CURRENT_PROFILE_NOT_FOUND_ID = "Current user profile not found with ID: ";
    public static final String ERR_CURRENT_USER_NOT_FOUND = "Current user not found";
    public static final String ERR_TARGET_USER_NOT_FOUND_ID = "Target user not found with ID: ";
    public static final String ERR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String ERR_ACCOUNT_PRIVATE = "This account is private";
}
