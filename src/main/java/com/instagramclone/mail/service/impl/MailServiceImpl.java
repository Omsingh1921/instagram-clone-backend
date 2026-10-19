package com.instagramclone.mail.service.impl;

import com.instagramclone.mail.service.interfaces.*;

import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.dto.EmailCommand;
import com.instagramclone.mail.enums.EmailPriority;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.security.otp.OtpProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements IMailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final MailProperties mailProperties;
    private final OtpProperties otpProperties;
    private final EmailDispatcher emailDispatcher;

    @Override
    @Async
    public void sendVerificationEmail(String recipientEmail, String username, String rawToken) {
        log.info("Preparing verification email dispatch for user [{}] to [{}]", username, recipientEmail);
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verifyUrl = mailProperties.getVerificationUrl() + "?token=" + encodedToken;

        Map<String, Object> vars = Map.of(
                "username", username != null ? username : "user",
                "verifyUrl", verifyUrl,
                "validityHours", Math.max(1, mailProperties.getVerificationTokenValidityMinutes() / 60)
        );

        emailDispatcher.dispatch(EmailCommand.builder()
                .emailType(EmailType.EMAIL_VERIFICATION)
                .recipientEmail(recipientEmail)
                .priority(EmailPriority.CRITICAL)
                .variables(vars)
                .build());
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String username, String rawToken) {
        log.info("Preparing password reset email dispatch for user [{}] to [{}]", username, recipientEmail);
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String resetUrl = mailProperties.getResetUrl() + "?token=" + encodedToken;

        Map<String, Object> vars = Map.of(
                "username", username != null ? username : "user",
                "resetUrl", resetUrl,
                "validityMinutes", mailProperties.getResetTokenValidityMinutes()
        );

        emailDispatcher.dispatch(EmailCommand.builder()
                .emailType(EmailType.PASSWORD_RESET)
                .recipientEmail(recipientEmail)
                .priority(EmailPriority.CRITICAL)
                .variables(vars)
                .build());
    }

    @Override
    @Async
    public void sendPasswordChangedAlert(String recipientEmail, String username) {
        log.info("Preparing password changed security alert dispatch for user [{}] to [{}]", username, recipientEmail);
        Map<String, Object> vars = Map.of(
                "username", username != null ? username : "user"
        );

        emailDispatcher.dispatch(EmailCommand.builder()
                .emailType(EmailType.PASSWORD_CHANGED)
                .recipientEmail(recipientEmail)
                .priority(EmailPriority.HIGH)
                .variables(vars)
                .build());
    }

    @Override
    public EmailDeliveryLog sendOtpEmail(String recipientEmail, String username, String otpCode) {
        log.info("Preparing OTP email dispatch for user [{}] to masked email [{}]", username, maskEmail(recipientEmail));

        int validityMinutes = (otpProperties != null) ? otpProperties.getValidityMinutes() : 5;

        Map<String, Object> vars = Map.of(
                "username", username != null ? username : "user",
                "otpCode", otpCode != null ? otpCode : "",
                "validityMinutes", validityMinutes
        );

        return emailDispatcher.dispatch(EmailCommand.builder()
                .emailType(EmailType.EMAIL_OTP)
                .recipientEmail(recipientEmail)
                .priority(EmailPriority.CRITICAL)
                .variables(vars)
                .build());
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
}
