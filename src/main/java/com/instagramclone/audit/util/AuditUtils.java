package com.instagramclone.audit.util;

import com.instagramclone.audit.constants.AuditConstants;

/**
 * Utility methods for sanitizing IP addresses and sensitive details in audit trails.
 */
public final class AuditUtils {

    private AuditUtils() {
        // Prevent instantiation
    }

    /**
     * Sanitizes client IP address, extracting first address from comma-separated forward headers.
     */
    public static String extractClientIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            return "0.0.0.0";
        }
        String ip = rawIp.trim();
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip.length() > AuditConstants.MAX_IP_LENGTH) {
            return ip.substring(0, AuditConstants.MAX_IP_LENGTH);
        }
        return ip;
    }

    /**
     * Truncates resource descriptor to fit audit schema constraints.
     */
    public static String truncateResource(String resource) {
        if (resource == null) {
            return "";
        }
        if (resource.length() <= AuditConstants.MAX_RESOURCE_LENGTH) {
            return resource;
        }
        return resource.substring(0, AuditConstants.MAX_RESOURCE_LENGTH);
    }
}
