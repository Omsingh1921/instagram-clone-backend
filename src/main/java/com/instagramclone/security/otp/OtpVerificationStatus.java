package com.instagramclone.security.otp;

/**
 * Result status for an OTP verification challenge attempt.
 */
public enum OtpVerificationStatus {
    /**
     * Code matched and has been atomically consumed.
     */
    SUCCESS,

    /**
     * Code does not match, but challenge remains active with remaining attempts.
     */
    INVALID_CODE,

    /**
     * Challenge burned due to reaching the maximum permitted incorrect attempts.
     */
    MAX_ATTEMPTS_EXCEEDED,

    /**
     * Challenge has expired or was not found / already consumed.
     */
    EXPIRED_OR_NOT_FOUND
}
