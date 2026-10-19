package com.instagramclone.story.service.interfaces;


import com.instagramclone.chat.dto.response.MessageResponse;
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

import java.util.List;
import java.util.UUID;

public interface IStoryService {

    StoryResponse createStory(CreateStoryRequest request);

    List<UserStoriesResponse> getStoryFeed();

    UserStoriesResponse getUserStories(UUID userId);

    void markStoryViewed(UUID storyId);

    void deleteStory(UUID storyId);

    int cleanupExpiredStories();

    List<StoryViewerResponse> getStoryViewers(UUID storyId);

    StoryLikeResponse toggleLikeStory(UUID storyId);

    HighlightSummaryResponse createHighlight(CreateHighlightRequest request);

    HighlightDetailResponse getHighlightById(UUID highlightId);

    List<HighlightSummaryResponse> getUserHighlights(UUID userId);

    HighlightSummaryResponse updateHighlight(UUID highlightId, UpdateHighlightRequest request);

    void deleteHighlight(UUID highlightId);

    MessageResponse replyToStory(UUID storyId, StoryReplyRequest request);

    StoryPollResponse voteOnPoll(UUID storyId, StoryPollVoteRequest request);
}
