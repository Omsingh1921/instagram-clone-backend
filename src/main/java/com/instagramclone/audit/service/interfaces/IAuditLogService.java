package com.instagramclone.audit.service.interfaces;

import java.util.UUID;

public interface IAuditLogService {
    void record(UUID userId, String action, String resourceType, UUID resourceId, String ipAddress, String metadata);
}
