package com.instagramclone.follow.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.follow.constants.FollowConstants;
import com.instagramclone.follow.dto.response.FollowRequestResponse;
import com.instagramclone.follow.dto.response.FollowResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.follow.service.interfaces.IFollowService;
import com.instagramclone.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(FollowConstants.BASE_PATH)
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    @RateLimit(category = FollowConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(FollowConstants.FOLLOW_USER_PATH)
    public ResponseEntity<ApiResponse<FollowResponse>> followUser(@PathVariable UUID userId) {
        FollowResponse response = followService.followUser(userId);
        String message = response.isRequested() ? FollowConstants.MSG_FOLLOW_REQUEST_SUBMITTED : FollowConstants.MSG_FOLLOWED_SUCCESSFULLY;
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping(FollowConstants.FOLLOW_USER_PATH)
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable UUID userId) {
        followService.unfollowUser(userId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_UNFOLLOWED_SUCCESSFULLY, null));
    }

    @DeleteMapping(FollowConstants.CANCEL_FOLLOW_REQUEST_PATH)
    public ResponseEntity<ApiResponse<Void>> cancelFollowRequest(@PathVariable UUID userId) {
        followService.cancelFollowRequest(userId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOW_REQUEST_CANCELLED, null));
    }

    @GetMapping(FollowConstants.ME_FOLLOW_REQUESTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<FollowRequestResponse>>> getPendingFollowRequests(
            @PageableDefault(size = FollowConstants.DEFAULT_PAGE_SIZE, sort = FollowConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowRequestResponse> requests = followService.getPendingFollowRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOW_REQUESTS_RETRIEVED, requests));
    }

    @PostMapping(FollowConstants.ACCEPT_FOLLOW_REQUEST_PATH)
    public ResponseEntity<ApiResponse<FollowResponse>> acceptFollowRequest(@PathVariable UUID requestId) {
        FollowResponse response = followService.acceptFollowRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOW_REQUEST_ACCEPTED, response));
    }

    @PostMapping(FollowConstants.REJECT_FOLLOW_REQUEST_PATH)
    public ResponseEntity<ApiResponse<Void>> rejectFollowRequestPost(@PathVariable UUID requestId) {
        followService.rejectFollowRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOW_REQUEST_REJECTED, null));
    }

    @DeleteMapping(FollowConstants.FOLLOW_REQUEST_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> rejectFollowRequestDelete(@PathVariable UUID requestId) {
        followService.rejectFollowRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOW_REQUEST_REJECTED, null));
    }

    @DeleteMapping(FollowConstants.REMOVE_FOLLOWER_PATH)
    public ResponseEntity<ApiResponse<Void>> removeFollower(@PathVariable UUID followerUserId) {
        followService.removeFollower(followerUserId);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOWER_REMOVED, null));
    }

    @GetMapping(FollowConstants.USER_FOLLOWERS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getFollowers(
            @PathVariable UUID userId,
            @PageableDefault(size = FollowConstants.DEFAULT_PAGE_SIZE, sort = FollowConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowUserResponse> followers = followService.getFollowers(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOWERS_RETRIEVED, followers));
    }

    @GetMapping(FollowConstants.USER_FOLLOWING_PATH)
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getFollowing(
            @PathVariable UUID userId,
            @PageableDefault(size = FollowConstants.DEFAULT_PAGE_SIZE, sort = FollowConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowUserResponse> following = followService.getFollowing(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(FollowConstants.MSG_FOLLOWING_RETRIEVED, following));
    }
}
