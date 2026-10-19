package com.instagramclone.mail.config;

import com.instagramclone.mail.enums.EmailCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private boolean enabled = true;
    private String from = "lastsingh008@gmail.com";
    private String fromName = "Instagram";
    private String verificationUrl = "http://localhost:5173/verify-email";
    private String resetUrl = "http://localhost:5173/reset-password";
    private String unsubscribeUrl = "http://localhost:5173/unsubscribe";
    private String unsubscribeSecret = "instagram-email-unsubscribe-hmac-sha256-secret-key-32bytes";
    private long unsubscribeTokenValidityDays = 30;
    private long verificationTokenValidityMinutes = 1440; // 24 hours
    private long resetTokenValidityMinutes = 30; // 30 minutes

    private Senders senders = new Senders();

    public SenderIdentity getSenderForCategory(EmailCategory category) {
        if (senders == null) {
            return new SenderIdentity(from, fromName);
        }
        return switch (category) {
            case SECURITY -> senders.getSecurity() != null ? senders.getSecurity() : new SenderIdentity(from, fromName);
            case ACCOUNT -> senders.getAccount() != null ? senders.getAccount() : new SenderIdentity(from, fromName);
            case SOCIAL_ACTIVITY ->
                senders.getActivity() != null ? senders.getActivity() : new SenderIdentity(from, fromName);
            case MESSAGING ->
                senders.getMessaging() != null ? senders.getMessaging() : new SenderIdentity(from, fromName);
            case DIGEST_PRODUCT ->
                senders.getDigest() != null ? senders.getDigest() : new SenderIdentity(from, fromName);
        };
    }

    @Data
    public static class Senders {
        // private SenderIdentity security = new
        // SenderIdentity("security@instagram.com", "Instagram Security");
        // private SenderIdentity account = new SenderIdentity("no-reply@instagram.com",
        // "Instagram");
        // private SenderIdentity activity = new
        // SenderIdentity("activity@instagram.com", "Instagram");
        // private SenderIdentity messaging = new
        // SenderIdentity("messages@instagram.com", "Instagram Direct");
        // private SenderIdentity digest = new SenderIdentity("updates@instagram.com",
        // "Instagram Updates");

        private SenderIdentity security = new SenderIdentity("lastsingh008@gmail.com", "Instagram Security");
        private SenderIdentity account = new SenderIdentity("lastsingh008@gmail.com", "Instagram");
        private SenderIdentity activity = new SenderIdentity("lastsingh008@gmail.com", "Instagram");
        private SenderIdentity messaging = new SenderIdentity("lastsingh008@gmail.com", "Instagram Direct");
        private SenderIdentity digest = new SenderIdentity("lastsingh008@gmail.com", "Instagram Updates");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderIdentity {
        private String from;
        private String name;
    }
}
