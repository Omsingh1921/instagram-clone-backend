package com.instagramclone.common;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.event.DomainEvent;
import com.instagramclone.common.event.DomainEventConsumer;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DomainEventIdempotencyTest extends BaseIntegrationTest {

    @Autowired
    private DomainEventConsumer consumer;

    @Autowired
    private RedisUtility redisUtility;

    @Test
    @DisplayName("Kafka Event Idempotency: Deduplicates redelivered events using Redis 24h key")
    void testKafkaEventDeduplication() {
        UUID eventId = UUID.randomUUID();
        String idempotencyKey = CacheConstants.CACHE_IDEMPOTENCY_EVENT_PREFIX + eventId;

        DomainEvent event = DomainEvent.builder()
                .eventId(eventId.toString())
                .eventType("POST_CREATED")
                .actorId(UUID.randomUUID())
                .targetId(UUID.randomUUID())
                .build();

        try {
            // First delivery
            consumer.onKafkaEvent(event);

            // Key should exist in Redis
            Optional<String> val = redisUtility.get(idempotencyKey);
            assertThat(val).isPresent().contains("processed");

            // Second delivery (duplicate redelivery by Kafka)
            consumer.onKafkaEvent(event);

            // Value remains intact, deduplication was executed without throwing
            assertThat(redisUtility.get(idempotencyKey)).isPresent().contains("processed");
        } finally {
            redisUtility.delete(idempotencyKey);
        }
    }

    @Test
    @DisplayName("Kafka Event Idempotency: Gracefully handles null event or null eventId")
    void testNullEventGracefulHandling() {
        consumer.onKafkaEvent(null);
        consumer.onKafkaEvent(DomainEvent.builder().eventId(null).build());
    }
}
