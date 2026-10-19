package com.instagramclone.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    private String eventType;

    @Builder.Default
    private String eventVersion = "v1";

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    private String producer;

    private String aggregateType;

    private UUID aggregateId;

    private UUID correlationId;

    private T payload;
}
