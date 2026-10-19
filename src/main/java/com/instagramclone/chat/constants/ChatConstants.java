package com.instagramclone.chat.constants;

/**
 * Domain constants for direct messaging and real-time chat.
 */
public final class ChatConstants {

    private ChatConstants() {
        // Prevent instantiation
    }

    public static final int MAX_MESSAGE_LENGTH = 2000;
    public static final int MAX_MEDIA_URL_LENGTH = 1000;
    public static final int TYPING_TIMEOUT_SECONDS = 5;

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int DEFAULT_CONVERSATIONS_PAGE_SIZE = 20;
    public static final int DEFAULT_MESSAGES_PAGE_SIZE = 30;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String WEBSOCKET_TOPIC_PREFIX = "/topic/chat.";
    public static final String WEBSOCKET_TYPING_PREFIX = "/topic/typing.";
    public static final String WS_SEND_DESTINATION = "/chat.send/{conversationId}";
    public static final String WS_TYPING_DESTINATION = "/chat.typing/{conversationId}";
    public static final String WS_QUEUE_ERRORS = "/queue/errors";
    public static final String WS_USER_QUEUE_ERRORS = "/user/queue/errors";
    public static final String FIELD_TYPING = "typing";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/conversations";
    public static final String CONVERSATION_BY_ID_PATH = "/{conversationId}";
    public static final String CONVERSATION_MESSAGES_PATH = "/{conversationId}/messages";
    public static final String CONVERSATION_READ_PATH = "/{conversationId}/read";
    public static final String CONVERSATION_GROUP_PATH = "/group";
    public static final String CONVERSATION_GROUP_DETAILS_PATH = "/{conversationId}/group";
    public static final String CONVERSATION_MEMBERS_PATH = "/{conversationId}/members";
    public static final String CONVERSATION_MEMBER_BY_ID_PATH = "/{conversationId}/members/{memberId}";
    public static final String CONVERSATION_LEAVE_PATH = "/{conversationId}/leave";
    public static final String CONVERSATION_MESSAGE_BY_ID_PATH = "/{conversationId}/messages/{messageId}";
    public static final String CONVERSATION_MESSAGE_REACTIONS_PATH = "/{conversationId}/messages/{messageId}/reactions";
    public static final String PRESENCE_BASE_PATH = "/api/v1/users";
    public static final String USER_PRESENCE_PATH = "/{userId}/presence";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_MESSAGING = "messaging";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_UPDATED_AT = "updatedAt";
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_CONVERSATION_READY = "Conversation ready";
    public static final String MSG_CONVERSATIONS_RETRIEVED = "Conversations retrieved successfully";
    public static final String MSG_CONVERSATION_RETRIEVED = "Conversation retrieved successfully";
    public static final String MSG_MESSAGES_RETRIEVED = "Messages retrieved successfully";
    public static final String MSG_MESSAGE_SENT = "Message sent successfully";
    public static final String MSG_CONVERSATION_MARKED_READ = "Conversation marked as read";
    public static final String MSG_GROUP_CREATED = "Group conversation created successfully";
    public static final String MSG_GROUP_UPDATED = "Group conversation updated successfully";
    public static final String MSG_MEMBERS_ADDED = "Members added to group successfully";
    public static final String MSG_MEMBER_REMOVED = "Member removed from group successfully";
    public static final String MSG_LEFT_GROUP = "Left group successfully";
    public static final String MSG_MESSAGE_UNSENT = "Message unsent successfully";
    public static final String MSG_REACTION_ADDED = "Reaction added successfully";
    public static final String MSG_REACTION_REMOVED = "Reaction removed successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_CONVERSATION_NOT_FOUND = "Conversation not found with ID: ";
    public static final String ERR_NOT_PARTICIPANT = "You are not a participant in this conversation";
    public static final String ERR_CANNOT_CHAT_SELF = "Cannot create a conversation with yourself";
    public static final String ERR_CANNOT_CHAT_USER = "Cannot create conversation with this user";
    public static final String ERR_BLOCKED_USER = "You have blocked this user. Unblock them first to start a conversation.";
    public static final String ERR_RECIPIENT_NOT_FOUND = "Recipient user not found with ID: ";
    public static final String ERR_UNAUTHORIZED_SEND = "You are not authorized to send messages in this conversation";
    public static final String ERR_BLOCKED_CONVERSATION = "Cannot send messages in this conversation due to user block";
    public static final String ERR_EMPTY_MESSAGE = "Message must contain either text content or media";
    public static final String ERR_CURRENT_USER_NOT_FOUND = "Current user not found";
    public static final String ERR_SENDER_NOT_FOUND = "Sender not found";
    public static final String ERR_CONVERSATION_AND_USER_ID_NULL = "Conversation ID and user ID must not be null";
    public static final String ERR_MESSAGE_REQUEST_NULL = "Message request body must not be null";
    public static final String ERR_AUTH_REQUIRED_SEND_MESSAGE = "User must be authenticated to send messages";
    public static final String ERR_AUTH_REQUIRED_TYPING = "User must be authenticated to send typing indicators";
    public static final String ERR_UNAUTHORIZED_TYPING = "You are not authorized to send typing indicators in this conversation";
    public static final String ERR_BLOCKED_TYPING = "Cannot send typing indicators in this conversation due to user block";
    public static final String ERR_NOT_MEMBER = "You are not a member of this conversation";
    public static final String MSG_INVALID_CHAT_MESSAGE = "Message content cannot exceed 2000 characters";
    public static final String ERR_MESSAGE_NOT_FOUND = "Message not found with ID: ";
    public static final String ERR_NOT_MESSAGE_SENDER = "You can only unsend your own messages";
    public static final String ERR_ONLY_GROUP_ADMIN = "Only group admins can perform this action";
    public static final String ERR_NOT_GROUP = "This conversation is not a group";
    public static final String ERR_CANNOT_LEAVE_DIRECT = "Cannot leave a direct 1-on-1 conversation";
}
