package com.instagramclone.chat.util;

import java.util.UUID;

/**
 * Utility methods for direct chat message formatting and key generation.
 */
public final class ChatUtils {

    private ChatUtils() {
        // Prevent instantiation
    }

    /**
     * Generates a deterministic direct conversation lookup key between two user IDs.
     */
    public static String getDirectConversationKey(UUID user1, UUID user2) {
        if (user1 == null || user2 == null) {
            return "";
        }
        if (user1.compareTo(user2) < 0) {
            return user1 + ":" + user2;
        } else {
            return user2 + ":" + user1;
        }
    }

    /**
     * Truncates message content for inbox previews.
     */
    public static String truncatePreview(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /**
     * Verifies that two users are not identical for direct messaging.
     */
    public static boolean isSelfConversation(UUID senderId, UUID recipientId) {
        return senderId != null && senderId.equals(recipientId);
    }
}
