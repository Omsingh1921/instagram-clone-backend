package com.instagramclone.security.otp;

/**
 * Purpose separation for OTP challenges to prevent cross-purpose challenge hijacking or overwrite.
 */
public enum OtpPurpose {
    SIGNUP,
    REGISTER,
    LOGIN,
    ACCOUNT_VERIFICATION,
    FORGOT_PASSWORD,
    FORGET_PASSWORD,
    RESET_PASSWORD,
    EMAIL_CHANGE
}
