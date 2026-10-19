package com.instagramclone.admin.util;

import com.instagramclone.admin.enums.ReportStatus;

/**
 * Utility methods for moderation state transitions and audit log formatting.
 */
public final class AdminUtils {

    private AdminUtils() {
        // Prevent instantiation
    }

    /**
     * Checks if a moderation report has reached a terminal state (RESOLVED or DISMISSED).
     */
    public static boolean isTerminalReportStatus(ReportStatus status) {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }

    /**
     * Formats resolution notes with timestamp and resolving administrator identifier.
     */
    public static String formatResolutionNote(String adminUsername, String notes) {
        String admin = (adminUsername != null && !adminUsername.isBlank()) ? adminUsername.trim() : "ADMIN";
        String content = (notes != null) ? notes.trim() : "No notes provided";
        return "[" + admin + "] " + content;
    }
}
