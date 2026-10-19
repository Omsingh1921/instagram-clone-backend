package com.instagramclone.mail.service.impl;

import com.instagramclone.mail.service.interfaces.*;

import com.instagramclone.mail.config.MailProperties;
import com.instagramclone.mail.dto.EmailCommand;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.enums.EmailCategory;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.provider.IEmailProvider;
import com.instagramclone.mail.repository.EmailDeliveryLogRepository;
import com.instagramclone.mail.template.EmailTemplateRenderer;
import com.instagramclone.mail.template.RenderedEmail;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);
    private static final Duration SOCIAL_DEBOUNCE_WINDOW = Duration.ofMinutes(30);

    private final IEmailProvider emailProvider;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final IEmailPreferenceService emailPreferenceService;
    private final EmailDeliveryLogRepository emailDeliveryLogRepository;
    private final UserRepository userRepository;
    private final MailProperties mailProperties;
    private final RedisUtility redisUtility;
    private final com.instagramclone.mail.service.interfaces.IDynamicEmailConfigService dynamicEmailConfigService;

    /**
     * Centralized entry point for sending typed email commands.
     * Enforces recipient validation, user preference checks, social debouncing,
     * template rendering, sender segregation, bounded retries, and delivery audit logging.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailDeliveryLog dispatch(EmailCommand command) {
        if (command == null || command.getRecipientEmail() == null || command.getRecipientEmail().isBlank()) {
            log.warn("Cannot dispatch email: command or recipient email is null/empty");
            return null;
        }

        EmailType type = command.getEmailType();
        if (type == null) {
            log.warn("Cannot dispatch email to [{}]: email type is null", maskEmail(command.getRecipientEmail()));
            return null;
        }

        Map<String, Object> vars = command.getVariables() != null ? command.getVariables() : Map.of();

        // 1. Resolve User entity if available
        User recipientUser = null;
        if (command.getRecipientUserId() != null) {
            recipientUser = userRepository.findById(command.getRecipientUserId()).orElse(null);
        }
        if (recipientUser == null && command.getRecipientEmail() != null) {
            java.util.List<User> matchingUsers = userRepository.findAllByEmail(command.getRecipientEmail().trim().toLowerCase(java.util.Locale.ROOT));
            if (matchingUsers.size() == 1) {
                recipientUser = matchingUsers.get(0);
            }
        }

        // 2. Check User Preference Policy
        if (recipientUser != null && !emailPreferenceService.isEmailAllowed(recipientUser.getId(), type)) {
            log.info("Email [{}] skipped for user [{}] per notification preferences", type, recipientUser.getId());
            EmailDeliveryLog skippedLog = EmailDeliveryLog.builder()
                    .recipientEmail(command.getRecipientEmail())
                    .user(recipientUser)
                    .emailType(type)
                    .category(type.getCategory())
                    .subject(type.getDefaultSubject())
                    .status(EmailDeliveryStatus.SKIPPED_PREFERENCE)
                    .attempts(0)
                    .build();
            return emailDeliveryLogRepository.save(skippedLog);
        }

        // 3. Social Spam / Frequency Debouncing
        if (type.getCategory() == EmailCategory.SOCIAL_ACTIVITY && isDebounced(command, recipientUser, vars)) {
            log.info("Social email [{}] throttled/debounced to [{}] to prevent spamming", type, maskEmail(command.getRecipientEmail()));
            EmailDeliveryLog throttledLog = EmailDeliveryLog.builder()
                    .recipientEmail(command.getRecipientEmail())
                    .user(recipientUser)
                    .emailType(type)
                    .category(type.getCategory())
                    .subject(type.getDefaultSubject())
                    .status(EmailDeliveryStatus.SKIPPED_THROTTLED)
                    .attempts(0)
                    .build();
            return emailDeliveryLogRepository.save(throttledLog);
        }

        // 4. Generate Unsubscribe Link (only for non-mandatory emails)
        String unsubscribeUrl = null;
        if (!type.isMandatory() && recipientUser != null) {
            unsubscribeUrl = emailPreferenceService.generateUnsubscribeUrl(recipientUser.getId(), type);
        }

        // 5. Render Responsive HTML and Plain Text
        RenderedEmail rendered = emailTemplateRenderer.render(type, vars, unsubscribeUrl);

        // 6. Resolve Purpose-Segregated Sender Identity (Dynamic Admin Configured)
        MailProperties.SenderIdentity sender = dynamicEmailConfigService.resolveSender(type.getCategory());

        // 7. Initialize Delivery Log
        // SECRETS HYGIENE: Never persist live OTP credentials in database audit entities.
        // Use generic canonical subject for audit trail while preserving code in the outbound transport envelope.
        String persistedSubject = (type == EmailType.EMAIL_OTP)
                ? type.getDefaultSubject()
                : rendered.subject();

        EmailDeliveryLog deliveryLog = EmailDeliveryLog.builder()
                .recipientEmail(command.getRecipientEmail())
                .user(recipientUser)
                .emailType(type)
                .category(type.getCategory())
                .subject(persistedSubject)
                .status(EmailDeliveryStatus.PROCESSING)
                .attempts(1)
                .build();
        deliveryLog = emailDeliveryLogRepository.save(deliveryLog);

        // 8. Execute Delivery with Bounded Retries
        int maxRetries = Math.max(1, type.getMaxRetries());
        int attempt = 0;
        boolean delivered = false;
        String lastError = null;

        while (attempt < maxRetries && !delivered) {
            attempt++;
            try {
                if (!emailProvider.isAvailable()) {
                    System.out.println("\n========================================================");
                    System.out.println("  >>> [SIMULATED EMAIL] RECIPIENT: " + command.getRecipientEmail() + " | TYPE: " + type + " | OTP: " + vars.get("otpCode") + " <<<");
                    System.out.println("========================================================\n");
                    log.info("[SIMULATED EMAIL] Email provider not configured. Delivery simulated for [{}] to [{}]. OTP Code: [{}]",
                            type, command.getRecipientEmail(), vars.get("otpCode"));
                    deliveryLog.setStatus(EmailDeliveryStatus.DELIVERED_SIMULATED);
                    deliveryLog.setSentAt(LocalDateTime.now());
                    delivered = true;
                } else {
                    emailProvider.send(
                            command.getRecipientEmail(),
                            rendered.subject(),
                            rendered.htmlBody(),
                            rendered.textBody(),
                            sender.getFrom(),
                            sender.getName()
                    );
                    deliveryLog.setStatus(EmailDeliveryStatus.SENT_TO_PROVIDER);
                    deliveryLog.setSentAt(LocalDateTime.now());
                    delivered = true;
                    log.info("Dispatched [{}] to [{}] via provider", type, formatRecipientForLog(command.getRecipientEmail(), type));
                }
            } catch (Exception ex) {
                lastError = sanitizeErrorMessage(ex.getMessage(), command.getRecipientEmail(), vars);
                log.warn("Attempt {}/{} failed sending [{}] to [{}]: {}",
                        attempt, maxRetries, type, formatRecipientForLog(command.getRecipientEmail(), type), lastError);
                if (attempt < maxRetries) {
                    deliveryLog.setStatus(EmailDeliveryStatus.RETRYING);
                    deliveryLog.setAttempts(attempt);
                    deliveryLog.setLastError(lastError);
                    emailDeliveryLogRepository.save(deliveryLog);
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        deliveryLog.setAttempts(attempt);
        if (!delivered) {
            deliveryLog.setStatus(EmailDeliveryStatus.FAILED);
            deliveryLog.setLastError(lastError);
        }

        return emailDeliveryLogRepository.save(deliveryLog);
    }

    private String formatRecipientForLog(String email, EmailType type) {
        if (email == null) return "unknown";
        if (type != null && type.isSecurityCritical()) {
            return maskEmail(email);
        }
        return maskEmail(email);
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

    private String sanitizeErrorMessage(String error, String recipientEmail, Map<String, Object> vars) {
        if (error == null || error.isBlank()) return "Unknown delivery error";
        String sanitized = error;
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sanitized = sanitized.replace(recipientEmail, maskEmail(recipientEmail));
        }
        if (vars != null && vars.containsKey("otpCode")) {
            String otp = String.valueOf(vars.get("otpCode"));
            if (otp != null && !otp.isBlank()) {
                sanitized = sanitized.replace(otp, "[REDACTED_OTP]");
            }
        }
        // Also redact any arbitrary 6-digit numeric codes that might appear in SMTP error messages
        return sanitized.replaceAll("\\b\\d{6}\\b", "[REDACTED_OTP]");
    }

    private boolean isDebounced(EmailCommand command, User recipientUser, Map<String, Object> vars) {
        if (!redisUtility.isAvailable()) {
            return false;
        }

        Object actor = vars.get("actorUsername");
        if (actor == null) {
            actor = vars.get("actorId");
        }
        if (actor == null) {
            return false;
        }

        String target = String.valueOf(vars.getOrDefault("postId", vars.getOrDefault("targetId", "generic")));
        String recipientId = (recipientUser != null) ? recipientUser.getId().toString() : command.getRecipientEmail();
        String debounceKey = "debounce:mail:" + command.getEmailType().name() + ":" + actor + ":" + recipientId + ":" + target;

        boolean isFirst = redisUtility.setIfAbsent(debounceKey, "1", SOCIAL_DEBOUNCE_WINDOW);
        return !isFirst;
    }
}
