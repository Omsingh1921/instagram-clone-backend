package com.instagramclone.security.otp;

import lombok.Builder;
import lombok.Getter;

/**
 * Result data produced when a new OTP challenge is created.
 */
@Getter
@Builder
public class OtpChallengeResult {
    private final String rawOtp;
    private final int validitySeconds;
    private final int cooldownSeconds;
    private final int remainingHourlyResends;
}
