package com.instagramclone.notification.constants;

/**
 * Domain constants for in-app push and activity notifications.
 */
public final class NotificationConstants {

    private NotificationConstants() {
        // Prevent instantiation
    }

    public static final int NOTIFICATION_EXPIRY_DAYS = 30;
    public static final int GROUPING_WINDOW_MINUTES = 60;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    public static final String CACHE_UNREAD_COUNT_PREFIX = "notifications:unread:";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/notifications";
    public static final String UNREAD_COUNT_PATH = "/unread-count";
    public static final String NOTIFICATION_READ_PATH = "/{notificationId}/read";
    public static final String NOTIFICATION_BY_ID_PATH = "/{notificationId}";
    public static final String READ_ALL_PATH = "/read-all";
    public static final String CLEAR_READ_PATH = "/clear-read";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_NOTIFICATIONS_RETRIEVED = "Notifications retrieved successfully";
    public static final String MSG_UNREAD_COUNT_RETRIEVED = "Unread count retrieved successfully";
    public static final String MSG_NOTIFICATION_READ = "Notification marked as read";
    public static final String MSG_ALL_READ = "All notifications marked as read";
    public static final String MSG_NOTIFICATION_DELETED = "Notification deleted successfully";
    public static final String MSG_READ_NOTIFICATIONS_CLEARED = "Read notifications cleared successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_NOTIFICATION_NOT_FOUND = "Notification not found with ID: ";
    public static final String ERR_UNAUTHORIZED_MARK_READ = "You cannot mark another user's notification as read";
    public static final String ERR_UNAUTHORIZED_DELETE_NOTIFICATION = "You cannot delete another user's notification";
}
