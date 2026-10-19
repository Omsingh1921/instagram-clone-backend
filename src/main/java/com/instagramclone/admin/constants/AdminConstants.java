package com.instagramclone.admin.constants;

/**
 * Domain constants for Admin moderation, reports, and user account status enforcement.
 */
public final class AdminConstants {

    private AdminConstants() {
        // Prevent instantiation
    }

    public static final int MAX_REPORT_REASON_LENGTH = 1000;
    public static final int MAX_RESOLUTION_NOTES_LENGTH = 2000;
    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int DEFAULT_REPORT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1";
    public static final String REPORTS_PATH = "/reports";
    public static final String ADMIN_REPORTS_PATH = "/admin/reports";
    public static final String ADMIN_REPORT_BY_ID_PATH = "/admin/reports/{reportId}";
    public static final String ADMIN_USER_STATUS_PATH = "/admin/users/{userId}/status";
    public static final String ADMIN_USERS_PATH = "/admin/users";
    public static final String ADMIN_USER_ROLE_PATH = "/admin/users/{userId}/role";
    public static final String ADMIN_DASHBOARD_STATS_PATH = "/admin/dashboard/stats";
    public static final String ADMIN_STATS_PATH = "/admin/stats";
    public static final String ADMIN_EMAIL_CONFIGS_PATH = "/admin/email-configs";
    public static final String ADMIN_EMAIL_CONFIG_BY_CATEGORY_PATH = "/admin/email-configs/{category}";
    public static final String ADMIN_EMAIL_CONFIG_TEST_PATH = "/admin/email-configs/test";
    public static final String ADMIN_EMAIL_TEMPLATES_PATH = "/admin/email-templates";
    public static final String ADMIN_EMAIL_TEMPLATE_BY_TYPE_PATH = "/admin/email-templates/{emailType}";
    public static final String ADMIN_EMAIL_TEMPLATE_PREVIEW_PATH = "/admin/email-templates/preview";
    public static final String ADMIN_EMAIL_TEMPLATE_RESET_PATH = "/admin/email-templates/reset/{emailType}";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_REPORT_SUBMITTED = "Report submitted successfully";
    public static final String MSG_REPORTS_RETRIEVED = "Reports retrieved successfully";
    public static final String MSG_REPORT_RETRIEVED = "Report retrieved successfully";
    public static final String MSG_REPORT_UPDATED = "Report updated successfully";
    public static final String MSG_USERS_RETRIEVED = "Users retrieved successfully";
    public static final String MSG_USER_STATUS_UPDATED = "User status updated successfully";
    public static final String MSG_USER_ROLE_UPDATED = "User role updated successfully";
    public static final String MSG_STATS_RETRIEVED = "Admin dashboard statistics retrieved successfully";
    public static final String MSG_EMAIL_CONFIGS_RETRIEVED = "Email configurations retrieved successfully";
    public static final String MSG_EMAIL_CONFIG_RETRIEVED = "Email configuration retrieved successfully";
    public static final String MSG_EMAIL_CONFIG_UPDATED = "Email configuration updated successfully";
    public static final String MSG_TEST_EMAIL_SENT = "Test email dispatched successfully";
    public static final String MSG_EMAIL_TEMPLATES_RETRIEVED = "Email templates retrieved successfully";
    public static final String MSG_EMAIL_TEMPLATE_RETRIEVED = "Email template retrieved successfully";
    public static final String MSG_EMAIL_TEMPLATE_UPDATED = "Email template updated successfully";
    public static final String MSG_EMAIL_TEMPLATE_PREVIEWED = "Email template preview generated successfully";
    public static final String MSG_EMAIL_TEMPLATE_RESET = "Email template reset to default successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_REPORT_NOT_FOUND = "Report not found with ID: ";
    public static final String ERR_CANNOT_REPORT_SELF = "You cannot report your own content";
    public static final String ERR_REPORTER_NOT_FOUND = "Reporter user not found";
    public static final String ERR_REVIEWER_NOT_FOUND = "Reviewer user not found";
    public static final String ERR_USER_NOT_FOUND = "User not found with ID: ";
    public static final String ERR_CANNOT_MODIFY_OWN_ROLE = "Administrators cannot change their own role";
    public static final String ERR_CANNOT_MODIFY_OWN_STATUS = "Administrators cannot change their own account status";
    public static final String ERR_TARGET_CONTENT_NOT_FOUND = "Reported target content not found with ID: ";
}
