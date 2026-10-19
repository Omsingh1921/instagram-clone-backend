package com.instagramclone.user.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.story.constants.StoryConstants;
import com.instagramclone.user.dto.response.CloseFriendResponse;
import com.instagramclone.user.service.interfaces.ICloseFriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(StoryConstants.BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "Close Friends", description = "Endpoints for managing Instagram Close Friends list for private stories")
public class CloseFriendController {

    private final ICloseFriendService closeFriendService;

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(StoryConstants.CLOSE_FRIEND_BY_ID_PATH)
    @Operation(summary = "Add a user to Close Friends list")
    public ResponseEntity<ApiResponse<Void>> addCloseFriend(@PathVariable UUID friendId) {
        closeFriendService.addCloseFriend(friendId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_CLOSE_FRIEND_ADDED, null));
    }

    @DeleteMapping(StoryConstants.CLOSE_FRIEND_BY_ID_PATH)
    @Operation(summary = "Remove a user from Close Friends list")
    public ResponseEntity<ApiResponse<Void>> removeCloseFriend(@PathVariable UUID friendId) {
        closeFriendService.removeCloseFriend(friendId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_CLOSE_FRIEND_REMOVED, null));
    }

    @GetMapping(StoryConstants.CLOSE_FRIENDS_PATH)
    @Operation(summary = "Get list of users in Close Friends list")
    public ResponseEntity<ApiResponse<List<CloseFriendResponse>>> getCloseFriends() {
        List<CloseFriendResponse> response = closeFriendService.getCloseFriends();
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_CLOSE_FRIENDS_RETRIEVED, response));
    }
}
