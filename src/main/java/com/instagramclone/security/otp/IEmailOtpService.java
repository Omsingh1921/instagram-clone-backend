package com.instagramclone.security.otp;

import java.util.UUID;

/**
 * Service contract for generating, storing, verifying, and invalidating
 * short-lived email OTP challenges with atomic attempt tracking and abuse controls.
 */
public interface IEmailOtpService {

    /**
     * Creates a new cryptographically secure 6-digit OTP challenge for the given email and purpose.
     * Enforces resend cooldown and hourly rate cap before storing in Redis.
     *
     * @param email   user's email address (normalized)
     * @param userId  optional UUID of the associated user (can be null for staged registration)
     * @param purpose the intended purpose of the OTP (SIGNUP or ACCOUNT_VERIFICATION)
     * @return OtpChallengeResult containing the raw 6-digit OTP, validity, and cooldown metadata
     */
    OtpChallengeResult createOtpChallenge(String email, UUID userId, OtpPurpose purpose);

    /**
     * Atomically validates a presented OTP against the stored challenge for a given purpose.
     * Enforces constant-time comparison, increments failure counter on mismatch,
     * burns the challenge after reaching maximum attempts, and consumes the code on success.
     *
     * @param email        user's email address (normalized)
     * @param presentedOtp the 6-digit code submitted by the user
     * @param purpose      the intended purpose of the OTP
     * @return OtpVerificationResult indicating outcome, remaining attempts, and optional userId
     */
    OtpVerificationResult verifyOtp(String email, String presentedOtp, OtpPurpose purpose);

    /**
     * Returns remaining cooldown in seconds for requesting another code for a given purpose.
     *
     * @param email   user's email address
     * @param purpose the intended purpose of the OTP
     * @return seconds remaining, or 0 if cooldown has expired
     */
    int getRemainingCooldownSeconds(String email, OtpPurpose purpose);

    /**
     * Invalidates any active challenge and cooldown for the given email and purpose.
     *
     * @param email   user's email address
     * @param purpose the intended purpose of the OTP
     */
    void invalidateChallenge(String email, OtpPurpose purpose);

    // --- Backwards compatible default methods (defaulting to ACCOUNT_VERIFICATION) ---

    default OtpChallengeResult createOtpChallenge(String email, UUID userId) {
        return createOtpChallenge(email, userId, OtpPurpose.ACCOUNT_VERIFICATION);
    }

    default OtpVerificationResult verifyOtp(String email, String presentedOtp) {
        return verifyOtp(email, presentedOtp, OtpPurpose.ACCOUNT_VERIFICATION);
    }

    default int getRemainingCooldownSeconds(String email) {
        return getRemainingCooldownSeconds(email, OtpPurpose.ACCOUNT_VERIFICATION);
    }

    default void invalidateChallenge(String email) {
        invalidateChallenge(email, OtpPurpose.ACCOUNT_VERIFICATION);
    }
}
