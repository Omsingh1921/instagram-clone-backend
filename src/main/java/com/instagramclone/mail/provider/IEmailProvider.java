package com.instagramclone.mail.provider;

public interface IEmailProvider {

    /**
     * Delivers an email message with HTML and plain-text fallback.
     */
    void send(String recipientEmail, String subject, String htmlBody, String textBody, String fromEmail, String fromName);

    /**
     * Checks if the underlying email provider is available.
     */
    boolean isAvailable();
}
