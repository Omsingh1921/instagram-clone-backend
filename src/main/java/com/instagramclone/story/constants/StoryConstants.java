package com.instagramclone.story.constants;

/**
 * Domain constants for 24-hour ephemeral Stories and Story Highlights.
 */
public final class StoryConstants {

    private StoryConstants() {
        // Prevent instantiation
    }

    public static final int STORY_LIFETIME_HOURS = 24;
    public static final int MAX_STORIES_PER_USER_DAY = 100;
    public static final int MAX_CAPTION_LENGTH = 500;
    public static final int MAX_HIGHLIGHT_TITLE_LENGTH = 50;
    public static final int MAX_VIDEO_STORY_SECONDS = 60;
    public static final int DEFAULT_IMAGE_DISPLAY_SECONDS = 5;

    public static final String CACHE_STORY_PREFIX = "story:";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1";
    public static final String STORIES_PATH = "/stories";
    public static final String STORY_FEED_PATH = "/stories/feed";
    public static final String USER_STORIES_PATH = "/users/{userId}/stories";
    public static final String STORY_VIEWERS_PATH = "/stories/{storyId}/viewers";
    public static final String STORY_VIEW_PATH = "/stories/{storyId}/view";
    public static final String STORY_LIKE_PATH = "/stories/{storyId}/like";
    public static final String STORY_BY_ID_PATH = "/stories/{storyId}";
    public static final String HIGHLIGHTS_PATH = "/highlights";
    public static final String HIGHLIGHT_BY_ID_PATH = "/highlights/{highlightId}";
    public static final String USER_HIGHLIGHTS_PATH = "/users/{userId}/highlights";
    public static final String STORY_REPLY_PATH = "/stories/{storyId}/reply";
    public static final String CLOSE_FRIENDS_PATH = "/users/close-friends";
    public static final String CLOSE_FRIEND_BY_ID_PATH = "/users/close-friends/{friendId}";
    public static final String STORY_POLL_VOTE_PATH = "/stories/{storyId}/poll/vote";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_CONTENT_CREATE = "content-create";
    public static final String RATE_LIMIT_CONTENT_ENGAGEMENT = "content-engagement";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_STORY_CREATED = "Story created successfully";
    public static final String MSG_STORY_FEED_RETRIEVED = "Story feed retrieved successfully";
    public static final String MSG_USER_STORIES_RETRIEVED = "User stories retrieved successfully";
    public static final String MSG_STORY_VIEWERS_RETRIEVED = "Story viewers retrieved successfully";
    public static final String MSG_STORY_MARKED_VIEWED = "Story marked as viewed";
    public static final String MSG_STORY_DELETED = "Story deleted successfully";
    public static final String MSG_STORY_LIKED = "Story liked successfully";
    public static final String MSG_STORY_UNLIKED = "Story unliked successfully";
    public static final String MSG_HIGHLIGHT_CREATED = "Highlight created successfully";
    public static final String MSG_HIGHLIGHT_RETRIEVED = "Highlight retrieved successfully";
    public static final String MSG_HIGHLIGHTS_RETRIEVED = "User highlights retrieved successfully";
    public static final String MSG_HIGHLIGHT_UPDATED = "Highlight updated successfully";
    public static final String MSG_HIGHLIGHT_DELETED = "Highlight deleted successfully";
    public static final String MSG_STORY_REPLIED = "Reply sent successfully";
    public static final String MSG_CLOSE_FRIEND_ADDED = "User added to close friends";
    public static final String MSG_CLOSE_FRIEND_REMOVED = "User removed from close friends";
    public static final String MSG_CLOSE_FRIENDS_RETRIEVED = "Close friends retrieved successfully";
    public static final String MSG_POLL_VOTED = "Poll vote recorded successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_STORY_NOT_FOUND = "Story not found with ID: ";
    public static final String ERR_USER_NOT_FOUND = "User not found with ID: ";
    public static final String ERR_UNAUTHORIZED_DELETE = "You are not authorized to delete this story";
    public static final String ERR_UNAUTHORIZED_VIEWERS = "You are not authorized to view viewers for this story";
    public static final String MSG_INVALID_STORY_MEDIA = "Story media URL is invalid or exceeds allowed length";
    public static final String ERR_HIGHLIGHT_NOT_FOUND = "Highlight not found with ID: ";
    public static final String ERR_UNAUTHORIZED_HIGHLIGHT = "You are not authorized to modify this highlight";
    public static final String ERR_HIGHLIGHT_STORIES_EMPTY = "At least one story is required to create a highlight";
    public static final String ERR_CLOSE_FRIENDS_CANNOT_ADD_SELF = "You cannot add yourself to close friends";
    public static final String ERR_STORY_CLOSE_FRIENDS_RESTRICTED = "This story is only accessible to close friends";
    public static final String ERR_POLL_NOT_FOUND = "Poll not found for this story";
    public static final String ERR_ALREADY_VOTED = "You have already voted on this poll";
    public static final String ERR_INVALID_POLL_OPTION = "Invalid poll option selected";
}
