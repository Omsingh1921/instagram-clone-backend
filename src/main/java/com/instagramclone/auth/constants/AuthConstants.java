package com.instagramclone.auth.constants;

/**
 * Domain constants for the Authentication flow:
 * endpoints, rate limit keys, token expiries, response messages,
 * and service exception descriptions for AuthController and IAuthService.
 */
public final class AuthConstants {

    private AuthConstants() {
        // Prevent instantiation
    }

    // ==================== CONFIGURATION & EXPIRIES ====================
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 15;
    public static final int EMAIL_VERIFICATION_EXPIRY_HOURS = 24;
    public static final int SIGNUP_OTP_EXPIRY_MINUTES = 10;
    public static final int OTP_MAX_ATTEMPTS = 5;

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 150;
    public static final int MAX_TOKEN_LENGTH = 255;
    public static final int MAX_REFRESH_TOKEN_LENGTH = 500;

    // ==================== API ENDPOINT PATHS ====================
    public static final String AUTH_BASE_PATH = "/api/v1/auth";

    // ==================== CONTROLLER SUB-PATHS ====================
    public static final String PATH_SIGNUP_REQUEST_OTP = "/signup/request-otp";
    public static final String PATH_SIGNUP_VERIFY_OTP = "/signup/verify-otp";
    public static final String PATH_SIGNUP_RESEND_OTP = "/signup/resend-otp";
    public static final String PATH_REGISTER = "/register";
    public static final String PATH_LOGIN = "/login";
    public static final String PATH_REFRESH = "/refresh";
    public static final String PATH_LOGOUT = "/logout";
    public static final String PATH_LOGOUT_ALL = "/logout-all";
    public static final String PATH_VERIFY_EMAIL = "/verify-email";
    public static final String PATH_RESEND_VERIFICATION = "/resend-verification";
    public static final String PATH_VERIFY_OTP = "/verify-otp";
    public static final String PATH_RESEND_OTP = "/resend-otp";
    public static final String PATH_FORGOT_PASSWORD = "/forgot-password";
    public static final String PATH_RESET_PASSWORD = "/reset-password";
    public static final String PATH_UNSUBSCRIBE = "/unsubscribe";
    public static final String PATH_CONFIRM_EMAIL_CHANGE = "/confirm-email-change";
    public static final String PATH_CHECK_USERNAME = "/check-username";

    // ==================== SWAGGER & OPENAPI TAGS ====================
    public static final String TAG_AUTH = "Authentication";
    public static final String TAG_AUTH_DESCRIPTION = "Endpoints for user registration, login, token refresh, and logout";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_SIGNUP_REQUEST_OTP = "auth-signup-request-otp";
    public static final String RATE_LIMIT_SIGNUP_VERIFY_OTP = "auth-signup-verify-otp";
    public static final String RATE_LIMIT_REGISTER = "auth-register";
    public static final String RATE_LIMIT_LOGIN = "auth-login";
    public static final String RATE_LIMIT_REFRESH = "auth-refresh";
    public static final String RATE_LIMIT_VERIFY_EMAIL = "auth-verify-email";
    public static final String RATE_LIMIT_RESEND_VERIFICATION = "auth-resend-verification";
    public static final String RATE_LIMIT_VERIFY_OTP = "auth-verify-otp";
    public static final String RATE_LIMIT_RESEND_OTP = "auth-resend-otp";
    public static final String RATE_LIMIT_FORGOT_PASSWORD = "auth-forgot-password";
    public static final String RATE_LIMIT_RESET_PASSWORD = "auth-reset-password";
    public static final String RATE_LIMIT_UNSUBSCRIBE = "auth-unsubscribe";
    public static final String RATE_LIMIT_CONFIRM_EMAIL = "auth-confirm-email";
    public static final String RATE_LIMIT_CHECK_USERNAME = "auth-check-username";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_USERNAME_AVAILABLE = "Username is available";
    public static final String MSG_USERNAME_TAKEN = "Username is already taken";
    public static final String MSG_USERNAME_INVALID = "Username format is invalid";
    public static final String MSG_USERNAME_RESERVED = "Username is reserved and cannot be registered";
    public static final String MSG_SIGNUP_OTP_SENT = "If this email is eligible, a verification code has been sent.";
    public static final String MSG_EMAIL_VERIFIED_SUCCESS = "Email verified successfully";
    public static final String MSG_USER_REGISTERED_SUCCESS = "User registered successfully";
    public static final String MSG_LOGIN_SUCCESS = "Login successful";
    public static final String MSG_TOKEN_REFRESHED_SUCCESS = "Token refreshed successfully";
    public static final String MSG_LOGOUT_SUCCESS = "Logout successful";
    public static final String MSG_ALL_SESSIONS_LOGGED_OUT_SUCCESS = "All sessions logged out successfully";
    public static final String MSG_VERIFICATION_LINK_SENT = "If an account exists with this email, a verification link has been sent.";
    public static final String MSG_OTP_SENT_SUCCESS = "If an unverified account exists with this email, a verification code has been sent.";
    public static final String MSG_PASSWORD_RESET_LINK_SENT = "If an account exists with this email, a password reset link has been sent.";
    public static final String MSG_PASSWORD_RESET_SUCCESS = "Password reset successfully. Please log in with your new credentials.";
    public static final String MSG_EMAIL_CHANGE_CONFIRMED = "Email address updated successfully. Please log in with your new email.";
    public static final String MSG_UNSUBSCRIBE_PREFIX = "Successfully unsubscribed from ";
    public static final String MSG_UNSUBSCRIBE_SUFFIX = " notifications";

    public static String formatUnsubscribeSuccess(String emailTypeName) {
        return MSG_UNSUBSCRIBE_PREFIX + emailTypeName + MSG_UNSUBSCRIBE_SUFFIX;
    }

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_USER_NOT_FOUND_ID = "User not found with ID: ";
    public static final String ERR_INVALID_CREDENTIALS = "Invalid username/email or password";
    public static final String ERR_EMAIL_ALREADY_EXISTS = "Email is already registered";
    public static final String ERR_USERNAME_ALREADY_EXISTS = "Username is already taken";
    public static final String ERR_MAX_ACCOUNTS_EXCEEDED = "Maximum account limit reached for this email address";
    public static final String ERR_MAX_ACCOUNTS_REACHED = "Maximum number of accounts (3) reached for this email address. Please log in or use a different email.";
    public static final String ERR_INVALID_TOKEN = "Invalid or expired token";
    public static final String ERR_INVALID_OTP = "Invalid or expired verification code";
    public static final String ERR_ACCOUNT_LOCKED = "Account is temporarily locked due to too many failed attempts";
    public static final String ERR_ACCOUNT_DISABLED = "Account has been suspended or disabled";
    public static final String ERR_PASSWORDS_DO_NOT_MATCH = "New password and confirmation password do not match";
    public static final String ERR_SIGNUP_PROOF_REQUIRED = "Signup verification token is required";
    public static final String ERR_SIGNUP_PROOF_INVALID = "Signup verification proof is invalid or has expired";
    public static final String ERR_SIGNUP_TOKEN_EXPIRED_OR_USED = "Signup verification token is expired or has already been used";
    public static final String ERR_SIGNUP_TICKET_INVALID = "Invalid, expired, or already consumed signup verification token";
    public static final String ERR_VERIFY_EMAIL_BEFORE_LOGIN = "Please verify your email address before logging in.";
    public static final String ERR_INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String ERR_REFRESH_TOKEN_EXPIRED_OR_REVOKED = "Refresh token is expired or revoked";
    public static final String ERR_EMAIL_NOT_VERIFIED = "Email address is not verified";
    public static final String ERR_SESSION_REVOKED = "Session has been revoked";
    public static final String ERR_INVALID_VERIFICATION_TOKEN = "Invalid or expired verification token";
    public static final String ERR_VERIFICATION_TOKEN_USED = "This verification token has already been used";
    public static final String ERR_VERIFICATION_TOKEN_EXPIRED = "Verification token has expired. Please request a new verification email.";
    public static final String ERR_INVALID_PASSWORD_RESET_TOKEN = "Invalid or expired password reset token";
    public static final String ERR_PASSWORD_RESET_TOKEN_USED = "This password reset token has already been used";
    public static final String ERR_PASSWORD_RESET_TOKEN_EXPIRED = "Password reset token has expired. Please request a new password reset link.";
    public static final String ERR_NO_UNVERIFIED_ACCOUNT = "No unverified account found for this email address";
    public static final String ERR_NO_ACCOUNT_FOR_CHALLENGE = "No account found for this verification challenge";
    public static final String ERR_MULTIPLE_UNVERIFIED_ACCOUNTS = "Multiple unverified accounts exist. Please use the verification link sent to your email.";
    public static final String ERR_ACCOUNT_ALREADY_VERIFIED = "Account is already verified. Please log in.";
    public static final String ERR_PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
    public static final String ERR_USERNAME_TAKEN_FORMAT = "Username '%s' is already taken";
}
