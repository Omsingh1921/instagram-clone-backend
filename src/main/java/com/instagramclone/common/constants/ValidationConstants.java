package com.instagramclone.common.constants;

/**
 * Centralized Bean Validation constants, regex patterns, field bounds,
 * and canonical error messages for Instagram Clone backend request DTOs.
 */
public final class ValidationConstants {

    private ValidationConstants() {}

    // ==================== USERNAME ====================
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 30;
    public static final String USERNAME_REGEX = "^(?!.*\\.\\.)(?!\\.)(?!.*\\.$)[a-zA-Z0-9._]{3,30}$";
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String USERNAME_LENGTH_MESSAGE = "Username must be between 3 and 30 characters";
    public static final String USERNAME_PATTERN_MESSAGE =
            "Username can only contain letters, numbers, periods and underscores, cannot start or end with a period, and cannot contain consecutive periods";
    public static final String USERNAME_OR_EMAIL_REQUIRED = "Username or email is required";
    public static final String USERNAME_OR_EMAIL_MAX_LENGTH_MESSAGE = "Username or email cannot exceed 150 characters";
    public static final String EMAIL_OR_USERNAME_REQUIRED = "Email or username is required";
    public static final String EMAIL_OR_USERNAME_MAX_LENGTH_MESSAGE = "Email or username cannot exceed 150 characters";

    // ==================== EMAIL ====================
    public static final int EMAIL_MAX_LENGTH = 150;
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@(?!.*\\.\\.)[A-Za-z0-9][A-Za-z0-9.-]*\\.[A-Za-z]{2,}$";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID_MESSAGE = "Email must be a valid email address";
    public static final String EMAIL_FORMAT_MESSAGE = "Please provide a valid email address";
    public static final String EMAIL_MAX_LENGTH_MESSAGE = "Email cannot exceed 150 characters";
    public static final String NEW_EMAIL_REQUIRED = "New email address is required";

    // ==================== PASSWORD ====================
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,100}$";
    public static final String PASSWORD_PATTERN_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_LENGTH_MESSAGE = "Password must be at least 8 characters long";
    public static final String PASSWORD_LENGTH_RANGE_MESSAGE = "Password must be between 8 and 100 characters";
    public static final String PASSWORD_MAX_LENGTH_MESSAGE = "Password cannot exceed 100 characters";
    public static final String NEW_PASSWORD_REQUIRED = "New password is required";
    public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
    public static final String CURRENT_PASSWORD_VERIFY_IDENTITY = "Current password is required to verify identity";
    public static final String CONFIRM_PASSWORD_REQUIRED = "Confirm password is required";

    // ==================== TOKENS ====================
    public static final int TOKEN_MAX_LENGTH = 255;
    public static final int REFRESH_TOKEN_MAX_LENGTH = 500;
    public static final String SIGNUP_TOKEN_MAX_LENGTH_MESSAGE = "Signup token cannot exceed 255 characters";
    public static final String RESET_TOKEN_REQUIRED = "Reset token is required";
    public static final String RESET_TOKEN_MAX_LENGTH_MESSAGE = "Reset token cannot exceed 255 characters";
    public static final String CONFIRMATION_TOKEN_REQUIRED = "Confirmation token is required";
    public static final String CONFIRMATION_TOKEN_MAX_LENGTH_MESSAGE = "Token cannot exceed 255 characters";
    public static final String VERIFICATION_TOKEN_REQUIRED = "Verification token is required";
    public static final String REFRESH_TOKEN_REQUIRED = "Refresh token is required";
    public static final String REFRESH_TOKEN_MAX_LENGTH_MESSAGE = "Refresh token cannot exceed 500 characters";
    public static final String SIGNATURE_REQUIRED = "Signature token is required";
    public static final String SIGNATURE_MAX_LENGTH_MESSAGE = "Signature cannot exceed 255 characters";

    // ==================== OTP / VERIFICATION CODE ====================
    public static final int OTP_LENGTH = 6;
    public static final String OTP_REGEX = "^[0-9]{6}$";
    public static final String OTP_REQUIRED = "Verification code is required";
    public static final String OTP_FORMAT_MESSAGE = "Verification code must be exactly 6 numeric digits";

    // ==================== UUID ====================
    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    public static final String INVALID_UUID_MESSAGE = "Invalid UUID format";
    public static final String UUID_REQUIRED_MESSAGE = "UUID identifier is required";

    // ==================== PAGINATION & SORTING ====================
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String SORT_DIRECTION_REGEX = "^(ASC|DESC|asc|desc)$";
    public static final String SORT_DIRECTION_MESSAGE = "Sort direction must be either 'ASC' or 'DESC'";

    // ==================== CONTENT BOUNDS ====================
    public static final int DISPLAY_NAME_MAX_LENGTH = 100;
    public static final String DISPLAY_NAME_REGEX = "^$|^[\\p{L}0-9 ._'-]{1,100}$";
    public static final String DISPLAY_NAME_MAX_LENGTH_MESSAGE = "Display name cannot exceed 100 characters";
    public static final String DISPLAY_NAME_PATTERN_MESSAGE = "Display name contains invalid characters";

    public static final int BIO_MAX_LENGTH = 500;
    public static final String BIO_MAX_LENGTH_MESSAGE = "Bio cannot exceed 500 characters";

    public static final int WEBSITE_URL_MAX_LENGTH = 500;
    public static final String WEBSITE_URL_REGEX = "^$|^(https?://)?([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(/\\S*)?$";
    public static final String WEBSITE_URL_MAX_LENGTH_MESSAGE = "Website URL cannot exceed 500 characters";
    public static final String URL_INVALID_MESSAGE = "Must be a valid HTTP or HTTPS website URL";

    public static final String MEDIA_URL_REGEX = "^(/|https?://).*$";
    public static final String MEDIA_URL_OR_EMPTY_REGEX = "^$|^(/|https?://).*$";
    public static final String MEDIA_URL_INVALID_MESSAGE = "Media URL must be a valid path or HTTP/HTTPS URL";

    public static final int CAPTION_MAX_LENGTH = 2200;
    public static final String CAPTION_MAX_LENGTH_MESSAGE = "Caption cannot exceed 2200 characters";

    public static final int STORY_CAPTION_MAX_LENGTH = 500;
    public static final String STORY_CAPTION_MAX_LENGTH_MESSAGE = "Story caption cannot exceed 500 characters";

    public static final int COMMENT_MAX_LENGTH = 1000;
    public static final String COMMENT_REQUIRED = "Comment text is required";
    public static final String COMMENT_MAX_LENGTH_MESSAGE = "Comment must not exceed 1000 characters";

    public static final int MESSAGE_CONTENT_MAX_LENGTH = 2000;
    public static final String MESSAGE_CONTENT_MAX_LENGTH_MESSAGE = "Message content cannot exceed 2000 characters";

    public static final int MEDIA_URL_MAX_LENGTH = 1000;
    public static final String MEDIA_URL_REQUIRED = "Media URL is required";
    public static final String MEDIA_URL_MAX_LENGTH_MESSAGE = "Media URL cannot exceed 1000 characters";
    public static final String MEDIA_TYPE_REQUIRED = "Media type is required";
    public static final String DISPLAY_ORDER_REQUIRED = "Display order is required";
    public static final String DISPLAY_ORDER_MIN_MESSAGE = "Display order must be 0 or greater";
    public static final String WIDTH_MIN_MESSAGE = "Width must be at least 1 pixel";
    public static final String HEIGHT_MIN_MESSAGE = "Height must be at least 1 pixel";

    public static final int MAX_POST_MEDIA_ITEMS = 10;
    public static final String VIDEO_URL_REQUIRED = "Video URL is required";
    public static final String VIDEO_URL_MAX_LENGTH_MESSAGE = "Video URL cannot exceed 1000 characters";
    public static final String THUMBNAIL_URL_MAX_LENGTH_MESSAGE = "Thumbnail URL cannot exceed 1000 characters";
    public static final String DURATION_MIN_MESSAGE = "Duration must be 0 or greater";
    public static final String MESSAGE_TYPE_REQUIRED = "Message type is required";
    public static final String RECIPIENT_ID_UUID_MESSAGE = "Recipient ID must be a valid UUID";
    public static final String OTP_DIGITS_EXACT_MESSAGE = "Verification code must be exactly 6 digits";
    public static final String COMMENT_BLANK_MESSAGE = "Comment text cannot be blank";

    public static final int REPORT_REASON_MAX_LENGTH = 1000;
    public static final int RESOLUTION_NOTES_MAX_LENGTH = 2000;
    public static final String REPORT_REASON_REQUIRED = "Report reason is required";
    public static final String REPORT_REASON_MAX_LENGTH_MESSAGE = "Report reason cannot exceed 1000 characters";
    public static final String RESOLUTION_NOTES_MAX_LENGTH_MESSAGE = "Resolution notes cannot exceed 2000 characters";
    public static final String RESOLUTION_NOTES_MAX_1000_MESSAGE = "Resolution notes cannot exceed 1000 characters";

    public static final String RECIPIENT_ID_REQUIRED = "Recipient ID is required";
    public static final String USER_ID_REQUIRED = "User ID is required";
    public static final String STATUS_REQUIRED = "User status is required";
    public static final String EMAIL_TYPE_REQUIRED = "Email type is required";
    public static final String EXPIRES_REQUIRED = "Expiry timestamp is required";
    public static final String EXPIRES_POSITIVE_MESSAGE = "Expiry timestamp must be a positive number";

    public static final String DIGEST_FREQUENCY_REGEX = "^(?i)(DAILY|WEEKLY|NEVER)?$";
    public static final String DIGEST_FREQUENCY_MESSAGE = "Digest frequency must be DAILY, WEEKLY, or NEVER";
    public static final int DIGEST_FREQUENCY_MAX_LENGTH = 20;
    public static final String DIGEST_FREQUENCY_MAX_LENGTH_MESSAGE = "Digest frequency must not exceed 20 characters";

    // ==================== MEDIA LIMITS ====================
    public static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024L; // 10MB
    public static final long MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024L; // 100MB
    public static final long MAX_IMAGE_PIXELS = 40_000_000L; // 40 Megapixels decompression bomb protection
    public static final String TRIM_SUCCESS_MESSAGE = "Trimmed successfully";
}
