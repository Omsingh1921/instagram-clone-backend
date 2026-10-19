package com.instagramclone.common.event;

import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.utility.KafkaUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publishes asynchronous domain events adhering to Sections 10 & 11 and Section 49.9.
 * Dispatches via Spring ApplicationEventPublisher and non-blocking KafkaUtility.
 */
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaUtility kafkaUtility;

    @Async
    public void publish(String eventType, UUID actorId, UUID targetId, String entityType, String details) {
        DomainEvent event = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .actorId(actorId)
                .targetId(targetId)
                .entityType(entityType)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();

        // 1. Publish internal Spring application event
        applicationEventPublisher.publishEvent(event);

        // 2. Publish to Kafka via KafkaUtility
        kafkaUtility.publish(KafkaConstants.DEFAULT_TOPIC, eventType, event);
    }
}
