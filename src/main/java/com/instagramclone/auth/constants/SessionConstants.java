package com.instagramclone.auth.constants;

/**
 * Domain constants for the Session Management flow:
 * endpoints, rate limit keys, response messages,
 * and service exception descriptions for SessionController and ISessionService.
 */
public final class SessionConstants {

    private SessionConstants() {
        // Prevent instantiation
    }

    // ==================== API ENDPOINT PATHS ====================
    public static final String SESSIONS_BASE_PATH = "/api/v1/sessions";
    public static final String PATH_SESSION_BY_ID = "/{sessionId}";
    public static final String PATH_SESSIONS_OTHER = "/other";

    // ==================== SWAGGER & OPENAPI TAGS ====================
    public static final String TAG_SESSION_MANAGEMENT = "Session Management";
    public static final String TAG_SESSION_MANAGEMENT_DESCRIPTION = "Endpoints for viewing and revoking active user sessions";

    // ==================== PROPERTIES & PAYLOAD KEYS ====================
    public static final String FIELD_REVOKED_COUNT = "revokedCount";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_SESSION_LIST = "session-list";
    public static final String RATE_LIMIT_SESSION_REVOKE = "session-revoke";
    public static final String RATE_LIMIT_SESSION_REVOKE_OTHER = "session-revoke-other";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_SESSION_REVOKED_SUCCESS = "Session revoked successfully";
    public static final String MSG_ACTIVE_SESSIONS_RETRIEVED = "Active sessions retrieved successfully";
    public static final String MSG_OTHER_SESSIONS_REVOKED = "All other sessions revoked successfully";
    public static final String MSG_SESSIONS_FETCHED_SUCCESS = "Sessions retrieved successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_SESSION_ID_NULL = "Session ID must not be null";
    public static final String ERR_SESSION_NOT_FOUND_ID = "Session not found with ID: ";
    public static final String ERR_CURRENT_SESSION_NOT_VERIFIED = "Current session identity could not be verified";
}
