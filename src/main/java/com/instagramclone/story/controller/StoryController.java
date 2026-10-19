package com.instagramclone.story.controller;

import com.instagramclone.chat.dto.response.MessageResponse;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.story.constants.StoryConstants;
import com.instagramclone.story.dto.request.CreateHighlightRequest;
import com.instagramclone.story.dto.request.CreateStoryRequest;
import com.instagramclone.story.dto.request.StoryPollVoteRequest;
import com.instagramclone.story.dto.request.StoryReplyRequest;
import com.instagramclone.story.dto.request.UpdateHighlightRequest;
import com.instagramclone.story.dto.response.HighlightDetailResponse;
import com.instagramclone.story.dto.response.HighlightSummaryResponse;
import com.instagramclone.story.dto.response.StoryLikeResponse;
import com.instagramclone.story.dto.response.StoryPollResponse;
import com.instagramclone.story.dto.response.StoryResponse;
import com.instagramclone.story.dto.response.StoryViewerResponse;
import com.instagramclone.story.dto.response.UserStoriesResponse;
import com.instagramclone.story.service.interfaces.IStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(StoryConstants.BASE_PATH)
@RequiredArgsConstructor
public class StoryController {

    private final IStoryService storyService;

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_CREATE, limit = 15, windowSeconds = 60)
    @PostMapping(StoryConstants.STORIES_PATH)
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(@Valid @RequestBody CreateStoryRequest request) {
        StoryResponse response = storyService.createStory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(StoryConstants.MSG_STORY_CREATED, response));
    }

    @GetMapping(StoryConstants.STORY_FEED_PATH)
    public ResponseEntity<ApiResponse<List<UserStoriesResponse>>> getStoryFeed() {
        List<UserStoriesResponse> feed = storyService.getStoryFeed();
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_STORY_FEED_RETRIEVED, feed));
    }

    @GetMapping(StoryConstants.USER_STORIES_PATH)
    public ResponseEntity<ApiResponse<UserStoriesResponse>> getUserStories(@PathVariable UUID userId) {
        UserStoriesResponse response = storyService.getUserStories(userId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_USER_STORIES_RETRIEVED, response));
    }

    @GetMapping(StoryConstants.STORY_VIEWERS_PATH)
    public ResponseEntity<ApiResponse<List<StoryViewerResponse>>> getStoryViewers(@PathVariable UUID storyId) {
        List<StoryViewerResponse> viewers = storyService.getStoryViewers(storyId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_STORY_VIEWERS_RETRIEVED, viewers));
    }

    @PostMapping(StoryConstants.STORY_VIEW_PATH)
    public ResponseEntity<ApiResponse<Void>> markStoryViewed(@PathVariable UUID storyId) {
        storyService.markStoryViewed(storyId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_STORY_MARKED_VIEWED, null));
    }

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(StoryConstants.STORY_LIKE_PATH)
    public ResponseEntity<ApiResponse<StoryLikeResponse>> toggleLikeStory(@PathVariable UUID storyId) {
        StoryLikeResponse response = storyService.toggleLikeStory(storyId);
        String message = response.isLiked() ? StoryConstants.MSG_STORY_LIKED : StoryConstants.MSG_STORY_UNLIKED;
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 30, windowSeconds = 60)
    @PostMapping(StoryConstants.STORY_REPLY_PATH)
    public ResponseEntity<ApiResponse<MessageResponse>> replyToStory(
            @PathVariable UUID storyId,
            @Valid @RequestBody StoryReplyRequest request) {
        MessageResponse response = storyService.replyToStory(storyId, request);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_STORY_REPLIED, response));
    }

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_ENGAGEMENT, limit = 60, windowSeconds = 60)
    @PostMapping(StoryConstants.STORY_POLL_VOTE_PATH)
    public ResponseEntity<ApiResponse<StoryPollResponse>> voteOnPoll(
            @PathVariable UUID storyId,
            @Valid @RequestBody StoryPollVoteRequest request) {
        StoryPollResponse response = storyService.voteOnPoll(storyId, request);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_POLL_VOTED, response));
    }

    @DeleteMapping(StoryConstants.STORY_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable UUID storyId) {
        storyService.deleteStory(storyId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_STORY_DELETED, null));
    }

    // ==================== STORY HIGHLIGHTS ====================

    @RateLimit(category = StoryConstants.RATE_LIMIT_CONTENT_CREATE, limit = 15, windowSeconds = 60)
    @PostMapping(StoryConstants.HIGHLIGHTS_PATH)
    public ResponseEntity<ApiResponse<HighlightSummaryResponse>> createHighlight(@Valid @RequestBody CreateHighlightRequest request) {
        HighlightSummaryResponse response = storyService.createHighlight(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(StoryConstants.MSG_HIGHLIGHT_CREATED, response));
    }

    @GetMapping(StoryConstants.HIGHLIGHT_BY_ID_PATH)
    public ResponseEntity<ApiResponse<HighlightDetailResponse>> getHighlightById(@PathVariable UUID highlightId) {
        HighlightDetailResponse response = storyService.getHighlightById(highlightId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_HIGHLIGHT_RETRIEVED, response));
    }

    @GetMapping(StoryConstants.USER_HIGHLIGHTS_PATH)
    public ResponseEntity<ApiResponse<List<HighlightSummaryResponse>>> getUserHighlights(@PathVariable UUID userId) {
        List<HighlightSummaryResponse> response = storyService.getUserHighlights(userId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_HIGHLIGHTS_RETRIEVED, response));
    }

    @PutMapping(StoryConstants.HIGHLIGHT_BY_ID_PATH)
    public ResponseEntity<ApiResponse<HighlightSummaryResponse>> updateHighlight(
            @PathVariable UUID highlightId,
            @Valid @RequestBody UpdateHighlightRequest request) {
        HighlightSummaryResponse response = storyService.updateHighlight(highlightId, request);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_HIGHLIGHT_UPDATED, response));
    }

    @DeleteMapping(StoryConstants.HIGHLIGHT_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable UUID highlightId) {
        storyService.deleteHighlight(highlightId);
        return ResponseEntity.ok(ApiResponse.success(StoryConstants.MSG_HIGHLIGHT_DELETED, null));
    }
}
