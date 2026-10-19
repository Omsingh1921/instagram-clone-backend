package com.instagramclone.mail.service.interfaces;

import com.instagramclone.mail.entity.EmailDeliveryLog;

public interface IMailService {

    /**
     * Dispatches an account verification email with a cryptographically secure token.
     */
    void sendVerificationEmail(String recipientEmail, String username, String rawToken);

    /**
     * Dispatches a single-use password reset link.
     */
    void sendPasswordResetEmail(String recipientEmail, String username, String rawToken);

    /**
     * Dispatches a security notification alerting the user that their password was changed.
     */
    void sendPasswordChangedAlert(String recipientEmail, String username);

    /**
     * Dispatches a 6-digit email OTP verification code with security card styling.
     * <p>
     * <b>ARCHITECTURAL DISPATCH CONTRACT (Phase 2 &amp; Phase 3):</b><br>
     * For any given OTP challenge (registration or resend), the caller MUST invoke exactly ONE
     * dispatch mechanism: either this synchronous method OR asynchronous event publishing via
     * {@code EmailEventPublisher.publishAfterCommit()}. The caller must NEVER invoke both for the
     * same OTP challenge, as event deduplication only guards against cross-node Spring/Kafka replays
     * and cannot detect prior synchronous direct service invocations.
     *
     * @param recipientEmail destination email address
     * @param username user handle for personalization
     * @param otpCode 6-digit numeric OTP code
     * @return EmailDeliveryLog containing delivery audit status (SENT_TO_PROVIDER, DELIVERED_SIMULATED, or FAILED)
     */
    EmailDeliveryLog sendOtpEmail(String recipientEmail, String username, String otpCode);
}
