package com.instagramclone.audit.constants;

/**
 * Domain constants for security audit logging and compliance event records.
 */
public final class AuditConstants {

    private AuditConstants() {
        // Prevent instantiation
    }

    public static final int MAX_ACTION_LENGTH = 100;
    public static final int MAX_RESOURCE_LENGTH = 255;
    public static final int MAX_IP_LENGTH = 45;
    public static final int RETENTION_DAYS = 90;

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String ACTION_USER_REGISTERED = "USER_REGISTERED";
}
