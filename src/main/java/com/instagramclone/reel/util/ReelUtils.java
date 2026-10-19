package com.instagramclone.reel.util;

import com.instagramclone.reel.constants.ReelConstants;

/**
 * Utility methods for Reel validation, audio metadata, and completion calculation.
 */
public final class ReelUtils {

    private ReelUtils() {
        // Prevent instantiation
    }

    /**
     * Resolves audio title, defaulting to creator original audio if unspecified.
     */
    public static String resolveAudioTitle(String audioTitle, String creatorUsername) {
        if (audioTitle != null && !audioTitle.isBlank()) {
            return audioTitle.trim();
        }
        if (creatorUsername != null && !creatorUsername.isBlank()) {
            return creatorUsername.trim() + " • " + ReelConstants.DEFAULT_AUDIO_TITLE;
        }
        return ReelConstants.DEFAULT_AUDIO_TITLE;
    }

    /**
     * Validates whether reel duration is within permissible bounds.
     */
    public static boolean isValidDuration(Integer durationSeconds) {
        return durationSeconds != null
                && durationSeconds >= ReelConstants.MIN_DURATION_SECONDS
                && durationSeconds <= ReelConstants.MAX_DURATION_SECONDS;
    }

    /**
     * Calculates the watch completion percentage (capped at 100%).
     */
    public static double calculateCompletionRate(long watchTimeSeconds, int totalDurationSeconds) {
        if (totalDurationSeconds <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (watchTimeSeconds * 100.0) / totalDurationSeconds);
    }
}
