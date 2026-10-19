package com.instagramclone.feed.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.feed.constants.FeedConstants;
import com.instagramclone.feed.service.interfaces.IFeedService;
import com.instagramclone.post.dto.response.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(FeedConstants.BASE_PATH)
@RequiredArgsConstructor
public class FeedController {

    private final IFeedService feedService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getHomeFeed(
            @PageableDefault(size = FeedConstants.DEFAULT_HOME_PAGE_SIZE, sort = FeedConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> feed = feedService.getHomeFeed(pageable);
        return ResponseEntity.ok(ApiResponse.success(FeedConstants.MSG_HOME_FEED_RETRIEVED, feed));
    }

    @GetMapping(FeedConstants.EXPLORE_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getExploreFeed(
            @PageableDefault(size = FeedConstants.DEFAULT_EXPLORE_PAGE_SIZE, sort = FeedConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> explore = feedService.getExploreFeed(pageable);
        return ResponseEntity.ok(ApiResponse.success(FeedConstants.MSG_EXPLORE_FEED_RETRIEVED, explore));
    }
}
