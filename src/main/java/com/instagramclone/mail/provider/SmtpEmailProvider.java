package com.instagramclone.mail.provider;

import com.instagramclone.mail.config.MailProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailProvider implements IEmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final MailProperties mailProperties;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public boolean isAvailable() {
        return mailProperties.isEnabled() && mailSender != null;
    }

    @Override
    public void send(String recipientEmail, String subject, String htmlBody, String textBody, String fromEmail, String fromName) {
        if (!isAvailable()) {
            log.info("Email delivery disabled or mail sender unavailable. Simulated delivery for [{}] to [{}] from [{} <{}>]",
                    sanitizeSubject(subject), maskEmail(recipientEmail), fromName, fromEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String effectiveFrom = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : mailProperties.getFrom();
            String effectiveName = (fromName != null && !fromName.isBlank()) ? fromName : mailProperties.getFromName();

            helper.setFrom(new InternetAddress(effectiveFrom, effectiveName));
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(message);
            log.info("Successfully dispatched email via SMTP to [{}] with subject [{}]", maskEmail(recipientEmail), sanitizeSubject(subject));
        } catch (Exception ex) {
            String sanitizedError = sanitizeErrorMessage(ex.getMessage(), recipientEmail);
            log.error("SMTP delivery failed for [{}] to [{}]: {}", sanitizeSubject(subject), maskEmail(recipientEmail), sanitizedError);
            throw new RuntimeException("SMTP delivery failed: " + sanitizedError);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private String sanitizeSubject(String subject) {
        if (subject == null) return "";
        return subject.replaceAll("\\b\\d{6}\\b", "[REDACTED_OTP]");
    }

    private String sanitizeErrorMessage(String error, String recipientEmail) {
        if (error == null || error.isBlank()) return "Unknown SMTP transmission error";
        String sanitized = error;
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sanitized = sanitized.replace(recipientEmail, maskEmail(recipientEmail));
        }
        return sanitized.replaceAll("\\b\\d{6}\\b", "[REDACTED_OTP]");
    }
}
