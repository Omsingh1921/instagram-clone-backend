package com.instagramclone.security.otp;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Result data produced when evaluating an OTP verification attempt.
 */
@Getter
@Builder
public class OtpVerificationResult {
    private final OtpVerificationStatus status;
    private final int remainingAttempts;
    private final UUID userId;

    public boolean isSuccessful() {
        return status == OtpVerificationStatus.SUCCESS;
    }
}
