package com.instagramclone.common.constants;

/**
 * Shared API and header constants adhering to Section 50.4 (Common Constants).
 */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_V1_PREFIX = "/api/v1";
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    public static final String HEALTH_PATH = "/health";
    public static final String HEALTH_ENDPOINT = "/api/v1/health";
    public static final String MSG_HEALTH_OPERATIONAL = "System foundation operational";

    // ==================== SHARED PAGINATION & SORTING ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";
    public static final int DEFAULT_PAGE_SIZE_10 = 10;
    public static final int DEFAULT_PAGE_SIZE_20 = 20;
    public static final String EMPTY_STRING = "";

    // ==================== COMMON QUERY PARAMETERS ====================
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_Q = "q";

    // ==================== SHARED ERROR MESSAGES & CODES ====================
    public static final String ERR_VALIDATION_FAILED = "Validation failed";
    public static final String ERR_ACCESS_DENIED = "You do not have permission to access this resource";
    public static final String ERR_CODE_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ERR_INVALID_ARGUMENT = "Invalid argument";
    public static final String ERR_CODE_BAD_REQUEST = "BAD_REQUEST";
    public static final String ERR_INTERNAL_ERROR = "An unexpected internal error occurred. Please try again later.";
    public static final String ERR_CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    // ==================== HEALTH STATUS & INFRASTRUCTURE KEYS ====================
    public static final String APP_NAME = "Instagram Clone Backend Foundation";
    public static final String STATUS_UP = "UP";
    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String STATUS_STANDBY_DISCONNECTED = "STANDBY / DISCONNECTED";
    public static final String STATUS_CONFIGURED = "CONFIGURED";
    public static final String KEY_APPLICATION = "application";
    public static final String KEY_STATUS = "status";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_JAVA_VERSION = "javaVersion";
    public static final String KEY_SPRING_BOOT_VERSION = "springBootVersion";
    public static final String KEY_DATABASE = "database";
    public static final String KEY_DATABASE_PRODUCT = "databaseProduct";
    public static final String KEY_DATABASE_VERSION = "databaseVersion";
    public static final String KEY_REDIS = "redis";
    public static final String KEY_PING = "ping";
    public static final String KEY_NOTE = "note";
    public static final String KEY_ERROR = "error";
    public static final String KEY_KAFKA = "kafka";
    public static final String KEY_BOOTSTRAP_SERVERS = "bootstrapServers";
    public static final String KEY_WEBSOCKET = "websocket";
    public static final String KEY_ENDPOINT = "endpoint";
    public static final String KEY_MESSAGE_BROKER = "messageBroker";
    public static final String WS_ENDPOINT_STOMP = "/ws";
    public static final String WS_BROKER_DESCRIPTION = "STOMP (/topic, /queue, /app)";
    public static final String NOTE_REDIS_STANDBY = "Local Redis not running or reachable";
    public static final String PROPERTY_JAVA_VERSION = "java.version";
}
