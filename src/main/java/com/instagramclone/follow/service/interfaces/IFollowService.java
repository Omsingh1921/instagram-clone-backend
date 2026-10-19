package com.instagramclone.follow.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.follow.dto.response.FollowResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFollowService {

    FollowResponse followUser(UUID targetUserId);

    void unfollowUser(UUID targetUserId);

    boolean isFollowing(UUID followerId, UUID targetUserId);

    PageResponse<FollowUserResponse> getFollowers(UUID userId, Pageable pageable);

    PageResponse<FollowUserResponse> getFollowing(UUID userId, Pageable pageable);

    long getFollowerCount(UUID userId);

    long getFollowingCount(UUID userId);

    List<UUID> getFollowingIds(UUID followerId);

    com.instagramclone.common.response.PageResponse<com.instagramclone.follow.dto.response.FollowRequestResponse> getPendingFollowRequests(org.springframework.data.domain.Pageable pageable);

    FollowResponse acceptFollowRequest(UUID requestId);

    void rejectFollowRequest(UUID requestId);

    void cancelFollowRequest(UUID targetUserId);

    void removeFollower(UUID followerUserId);
}
