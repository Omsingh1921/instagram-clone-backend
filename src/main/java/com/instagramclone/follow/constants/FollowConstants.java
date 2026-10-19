package com.instagramclone.follow.constants;

/**
 * Domain constants for follow relations, requests, and follower counts.
 */
public final class FollowConstants {

    private FollowConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String CACHE_FOLLOWERS_COUNT_PREFIX = "followers:count:";
    public static final String CACHE_FOLLOWING_COUNT_PREFIX = "following:count:";
    public static final String CACHE_MUTUAL_FOLLOW_PREFIX = "follow:mutual:";

    // ==================== ENDPOINT PATHS ====================
    public static final String BASE_PATH = "/api/v1/users";
    public static final String FOLLOW_USER_PATH = "/{userId}/follow";
    public static final String CANCEL_FOLLOW_REQUEST_PATH = "/{userId}/follow-request";
    public static final String ME_FOLLOW_REQUESTS_PATH = "/me/follow-requests";
    public static final String ACCEPT_FOLLOW_REQUEST_PATH = "/me/follow-requests/{requestId}/accept";
    public static final String REJECT_FOLLOW_REQUEST_PATH = "/me/follow-requests/{requestId}/reject";
    public static final String FOLLOW_REQUEST_BY_ID_PATH = "/me/follow-requests/{requestId}";
    public static final String REMOVE_FOLLOWER_PATH = "/me/followers/{followerUserId}";
    public static final String USER_FOLLOWERS_PATH = "/{userId}/followers";
    public static final String USER_FOLLOWING_PATH = "/{userId}/following";

    // ==================== RATE LIMIT CATEGORIES ====================
    public static final String RATE_LIMIT_CONTENT_ENGAGEMENT = "content-engagement";

    // ==================== SORT PROPERTIES ====================
    public static final String SORT_BY_CREATED_AT = "createdAt";

    // ==================== CONTROLLER SUCCESS MESSAGES ====================
    public static final String MSG_FOLLOW_REQUEST_SUBMITTED = "Follow request submitted";
    public static final String MSG_FOLLOWED_SUCCESSFULLY = "Successfully followed user";
    public static final String MSG_UNFOLLOWED_SUCCESSFULLY = "Successfully unfollowed user";
    public static final String MSG_FOLLOW_REQUEST_CANCELLED = "Follow request cancelled";
    public static final String MSG_FOLLOW_REQUESTS_RETRIEVED = "Follow requests retrieved successfully";
    public static final String MSG_FOLLOW_REQUEST_ACCEPTED = "Follow request accepted";
    public static final String MSG_FOLLOW_REQUEST_REJECTED = "Follow request rejected";
    public static final String MSG_FOLLOWER_REMOVED = "Follower removed successfully";
    public static final String MSG_FOLLOWERS_RETRIEVED = "Followers retrieved successfully";
    public static final String MSG_FOLLOWING_RETRIEVED = "Following retrieved successfully";

    // ==================== SERVICE & BUSINESS ERROR MESSAGES ====================
    public static final String ERR_CANNOT_FOLLOW_SELF = "You cannot follow yourself";
    public static final String ERR_USERS_CANNOT_FOLLOW_SELF = "Users cannot follow themselves";
    public static final String ERR_CANNOT_FOLLOW_USER = "You cannot follow this user";
    public static final String ERR_BLOCKED_USER = "You have blocked this user. Unblock them first to follow.";
    public static final String ERR_ALREADY_FOLLOWING = "You are already following this user";
    public static final String ERR_FOLLOW_REQUEST_PENDING = "Follow request already pending";
    public static final String ERR_NOT_FOLLOWING = "You are not following this user";
    public static final String ERR_FOLLOW_REQUEST_NOT_FOUND = "Follow request not found";
    public static final String ERR_FOLLOW_REQUEST_NOT_FOUND_ID = "Follow request not found with ID: ";
    public static final String ERR_UNAUTHORIZED_ACCEPT_REQUEST = "You are not authorized to accept this follow request";
    public static final String ERR_REQUEST_NOT_PENDING = "Follow request is not pending";
    public static final String ERR_NOT_A_FOLLOWER = "This user is not following you";
    public static final String ERR_USER_NOT_FOUND = "User not found with ID: ";
    public static final String ERR_USER_NOT_FOUND_ID = "User not found with ID: ";
    public static final String ERR_TARGET_USER_NOT_FOUND_ID = "Target user not found with ID: ";
    public static final String ERR_CURRENT_USER_NOT_FOUND = "Current user not found";
    public static final String ERR_UNAUTHORIZED_REJECT_REQUEST = "You are not authorized to reject this follow request";
    public static final String MSG_INVALID_TARGET_USER = "Target user ID must be a non-null valid UUID";
}
