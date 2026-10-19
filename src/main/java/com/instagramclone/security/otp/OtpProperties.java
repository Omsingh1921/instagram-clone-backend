package com.instagramclone.security.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email OTP verification.
 * Adheres to Section 49 / Section 5 of the Security Design Specification.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.auth.otp")
public class OtpProperties {

    /**
     * Dedicated secret key for HMAC-SHA256 calculation.
     * Injected via OTP_HMAC_SECRET environment variable in production.
     */
    private String secret = "instagram-clone-email-otp-hmac-sha256-secret-key-32bytes-default";

    /**
     * Number of digits in the OTP code (defaults to 6).
     */
    private int length = 6;

    /**
     * Lifetime of the OTP challenge in minutes (defaults to 5 minutes).
     */
    private int validityMinutes = 5;

    /**
     * Maximum incorrect attempts per challenge before the code is burned (defaults to 5).
     */
    private int maxAttempts = 5;

    /**
     * Minimum cooldown period between consecutive resend requests in seconds (defaults to 60s).
     */
    private int resendCooldownSeconds = 60;

    /**
     * Maximum allowed resend requests per email per hour (defaults to 5).
     */
    private int maxHourlyResends = 5;
}
