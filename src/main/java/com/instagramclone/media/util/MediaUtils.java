package com.instagramclone.media.util;

import com.instagramclone.media.constants.MediaConstants;

import java.util.Locale;

/**
 * Utility methods for validating media content types and formatting file sizes.
 */
public final class MediaUtils {

    private MediaUtils() {
        // Prevent instantiation
    }

    /**
     * Checks if a content type represents a permitted image format.
     */
    public static boolean isSupportedImage(String contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaConstants.ALLOWED_IMAGE_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if a content type represents a permitted video format.
     */
    public static boolean isSupportedVideo(String contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaConstants.ALLOWED_VIDEO_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Formats bytes into a human-readable size string (e.g. "4.2 MB").
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
