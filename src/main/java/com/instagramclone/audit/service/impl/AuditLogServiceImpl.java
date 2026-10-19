package com.instagramclone.audit.service.impl;

import com.instagramclone.audit.service.interfaces.IAuditLogService;

import com.instagramclone.audit.entity.AuditLog;
import com.instagramclone.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Structured audit logging for critical operations (Section 22).
 * Records actions asynchronously without blocking the user request.
 * Strictly adheres to security guidelines: never logs tokens, passwords, or secrets.
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private final AuditLogRepository auditLogRepository;

    @Async
    public void record(UUID userId, String action, String resourceType, UUID resourceId, String ipAddress, String metadata) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(ipAddress)
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("AUDIT_LOG: user=[{}] action=[{}] resource=[{}:{}] ip=[{}]",
                    userId, action, resourceType, resourceId, ipAddress);
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage());
        }
    }
}
