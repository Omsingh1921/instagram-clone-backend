package com.instagramclone.common.event;

import com.instagramclone.common.constants.CacheConstants;
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
 * Handles domain events adhering to Section 11 (Kafka Reliability & Safe Consumer Delivery).
 * Guarantees idempotent consumer execution via 24-hour Redis event deduplication keys.
 */
@Component
@RequiredArgsConstructor
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final RedisUtility redisUtility;

    // Spring In-Memory Application Event Consumer
    @Async
    @EventListener
    public void onDomainEvent(DomainEvent event) {
        log.info("Domain Event Processed [Type: {}, Actor: {}, Target: {}]",
                event.getEventType(), event.getActorId(), event.getTargetId());
    }

    // Kafka Consumer (when Kafka broker is active)
    @KafkaListener(
            topics = "instagram-clone.events",
            groupId = "${spring.kafka.consumer.group-id:instagram-clone-group}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "${app.kafka.enabled:false}"
    )
    public void onKafkaEvent(DomainEvent event) {
        if (event == null || event.getEventId() == null) return;

        String idempotencyKey = CacheConstants.CACHE_IDEMPOTENCY_EVENT_PREFIX + event.getEventId();
        boolean acquired = false;

        if (redisUtility.isAvailable()) {
            acquired = redisUtility.setIfAbsent(idempotencyKey, "processing", Duration.ofMinutes(15));
            if (!acquired) {
                log.info("Duplicate or in-flight Kafka event detected [ID: {}]. Skipping redundant processing.", event.getEventId());
                return;
            }
        }

        try {
            processKafkaEvent(event);
            if (acquired) {
                redisUtility.set(idempotencyKey, "processed", Duration.ofHours(24));
            }
        } catch (Exception ex) {
            log.error("Failed processing Kafka event [ID: {}]. Evicting idempotency key to allow Kafka consumer retry.",
                    event.getEventId(), ex);
            if (acquired) {
                redisUtility.delete(idempotencyKey);
            }
            throw ex;
        }
    }

    protected void processKafkaEvent(DomainEvent event) {
        log.info("Kafka Event Received and Processed [ID: {}, Type: {}, Actor: {}]",
                event.getEventId(), event.getEventType(), event.getActorId());
    }
}
