package com.instagramclone.user.service.interfaces;

import com.instagramclone.user.dto.request.ConfirmEmailChangeRequest;
import com.instagramclone.user.dto.response.EmailChangeResponse;
import com.instagramclone.user.dto.request.InitiateEmailChangeRequest;

import java.util.UUID;

public interface IEmailChangeService {

    /**
     * Initiates an email address change, verifying current password, generating a blind token,
     * dispatching verification to the new email, and alerting the old email.
     */
    EmailChangeResponse initiateEmailChange(UUID userId, InitiateEmailChangeRequest request);

    /**
     * Confirms the email change using the cryptographic blind token, switches the account email,
     * revokes active sessions, and dispatches completion alerts to both addresses.
     */
    void confirmEmailChange(ConfirmEmailChangeRequest request);

    /**
     * Cancels any pending email change requests for the user.
     */
    void cancelPendingEmailChange(UUID userId);
}
