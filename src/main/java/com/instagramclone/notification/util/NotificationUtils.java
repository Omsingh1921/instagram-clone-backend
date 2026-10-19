package com.instagramclone.notification.util;

import com.instagramclone.notification.enums.NotificationType;

import java.util.UUID;

/**
 * Utility methods for building user-friendly notification text and grouping keys.
 */
public final class NotificationUtils {

    private NotificationUtils() {
        // Prevent instantiation
    }

    /**
     * Builds descriptive notification text based on action type and actor username.
     */
    public static String buildNotificationMessage(String actorUsername, NotificationType type) {
        String actor = (actorUsername != null && !actorUsername.isBlank()) ? actorUsername : "Someone";
        if (type == null) {
            return actor + " interacted with your profile";
        }
        return switch (type) {
            case POST_LIKED -> actor + " liked your post.";
            case POST_COMMENTED -> actor + " commented on your post.";
            case FOLLOW -> actor + " started following you.";
            case USER_MENTIONED -> actor + " mentioned you.";
            case REEL_LIKED -> actor + " liked your reel.";
            case REEL_COMMENTED -> actor + " commented on your reel.";
            case STORY_LIKED -> actor + " liked your story.";
            default -> actor + " interacted with your profile.";
        };
    }

    /**
     * Generates a Redis/cache grouping key for collapsing rapid notifications from multiple users.
     */
    public static String generateGroupingKey(NotificationType type, UUID targetId) {
        if (type == null || targetId == null) {
            return "notif:general";
        }
        return "notif:" + type.name().toLowerCase() + ":" + targetId;
    }
}
