package com.instagramclone.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent implements Serializable {
    private String eventId;
    private String eventType;
    private UUID actorId;
    private UUID targetId;
    private String entityType;
    private LocalDateTime timestamp;
    private String details;
}
