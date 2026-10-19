package com.instagramclone.mail.service.interfaces;

import com.instagramclone.mail.dto.response.EmailPreferencesResponse;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.enums.EmailType;

import java.util.UUID;

public interface IEmailPreferenceService {

    /**
     * Retrieves the email preferences for a user, initializing defaults if none exist.
     */
    EmailPreferencesResponse getPreferences(UUID userId);

    /**
     * Updates email preferences for an authenticated user.
     */
    EmailPreferencesResponse updatePreferences(UUID userId, UpdateEmailPreferencesRequest request);

    /**
     * Checks if a specific email type is permitted by user settings and policy.
     * Mandatory security/account emails always return true regardless of settings.
     */
    boolean isEmailAllowed(UUID userId, EmailType emailType);

    /**
     * Generates an HMAC-SHA256 signed unsubscribe URL for an optional email type.
     */
    String generateUnsubscribeUrl(UUID userId, EmailType emailType);

    /**
     * Processes an unauthenticated unsubscribe request with HMAC-SHA256 signature verification.
     */
    boolean unsubscribeWithToken(UUID userId, EmailType emailType, long expires, String signature);
}
