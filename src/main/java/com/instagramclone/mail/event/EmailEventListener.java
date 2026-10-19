package com.instagramclone.mail.event;

import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.mail.service.impl.EmailDispatcher;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Consumes email delivery events asynchronously via Spring Events and Kafka.
 * Enforces distributed idempotency via Redis to prevent duplicate email transmissions between
 * Spring Events and Kafka.
 * <p>
 * <b>NOTE ON DISPATCH ARCHITECTURE:</b><br>
 * This event-driven deduplication mechanism does NOT track prior direct calls to
 * {@link com.instagramclone.mail.service.interfaces.IMailService#sendOtpEmail}. Services must invoke either
 * direct synchronous dispatch OR publish an event, never both for the same OTP challenge.
 */
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailEventListener.class);

    private final IMailService mailService;
    private final EmailDispatcher emailDispatcher;
    private final RedisUtility redisUtility;

    // In-memory Spring event consumer (asynchronous)
    @Async
    @EventListener
    public void onSpringEmailEvent(EmailEvent event) {
        if (event == null) return;
        log.info("Processing in-memory email event [ID: {}, Type: {}]", event.getEventId(), event.getEventType());
        processWithDeduplication(event, "SPRING_EVENT");
    }

    // Kafka consumer for multi-node deployments
    @KafkaListener(
            topics = KafkaConstants.DEFAULT_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:instagram-clone-group}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "${app.kafka.enabled:false}"
    )
    public void onKafkaEmailEvent(EmailEvent event) {
        if (event == null || event.getEventId() == null) return;
        log.info("Processing Kafka email event [ID: {}, Type: {}]", event.getEventId(), event.getEventType());
        processWithDeduplication(event, "KAFKA");
    }

    public void processWithDeduplication(EmailEvent event, String source) {
        String idempotencyKey = CacheConstants.CACHE_IDEMPOTENCY_EVENT_PREFIX + "email:" + event.getEventId();
        boolean acquired = false;

        if (redisUtility.isAvailable()) {
            acquired = redisUtility.setIfAbsent(idempotencyKey, "processing", Duration.ofMinutes(15));
            if (!acquired) {
                log.info("Duplicate or in-flight email event detected via {} [ID: {}]. Skipping redundant email dispatch.",
                        source, event.getEventId());
                return;
            }
        }

        try {
            deliverEmail(event);
            if (acquired) {
                redisUtility.set(idempotencyKey, "processed", Duration.ofHours(24));
            }
        } catch (Exception ex) {
            log.error("Failed executing email dispatch for event [ID: {}, Source: {}]: {}",
                    event.getEventId(), source, ex.getMessage(), ex);
            if (acquired) {
                redisUtility.delete(idempotencyKey);
            }
        }
    }

    private void deliverEmail(EmailEvent event) {
        if (event.getEmailType() != null) {
            emailDispatcher.dispatch(com.instagramclone.mail.dto.EmailCommand.builder()
                    .emailType(event.getEmailType())
                    .recipientEmail(event.getEmail())
                    .recipientUserId(event.getUserId())
                    .priority(event.getPriority() != null ? event.getPriority() : event.getEmailType().getDefaultPriority())
                    .variables(event.getVariables())
                    .correlationId(event.getEventId())
                    .build());
            return;
        }

        if (event.getEventType() != null) {
            switch (event.getEventType()) {
                case KafkaConstants.EVENT_EMAIL_VERIFICATION_REQUESTED ->
                        mailService.sendVerificationEmail(event.getEmail(), event.getUsername(), event.getRawToken());
                case KafkaConstants.EVENT_EMAIL_OTP_REQUESTED ->
                        mailService.sendOtpEmail(event.getEmail(), event.getUsername(), event.getRawToken());
                case KafkaConstants.EVENT_PASSWORD_RESET_REQUESTED ->
                        mailService.sendPasswordResetEmail(event.getEmail(), event.getUsername(), event.getRawToken());
                case KafkaConstants.EVENT_PASSWORD_CHANGED_SECURITY_ALERT ->
                        mailService.sendPasswordChangedAlert(event.getEmail(), event.getUsername());
                default ->
                        log.warn("Unrecognized email event type: {}", event.getEventType());
            }
        }
    }
}
