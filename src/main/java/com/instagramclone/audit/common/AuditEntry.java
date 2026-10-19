package com.instagramclone.audit.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common data structure for emitting security and audit events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntry {

    private UUID userId;
    private String action;
    private String resource;
    private String ipAddress;
    private Instant timestamp;
}
