package com.instagramclone.search.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.search.constants.SearchConstants;
import com.instagramclone.search.dto.request.AddSearchHistoryRequest;
import com.instagramclone.search.dto.response.ExploreItemResponse;
import com.instagramclone.search.dto.response.HashtagResponse;
import com.instagramclone.search.dto.response.RecentSearchResponse;
import com.instagramclone.search.service.interfaces.ISearchService;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(SearchConstants.BASE_PATH)
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @RateLimit(category = SearchConstants.RATE_LIMIT_SEARCH, limit = 40, windowSeconds = 60)
    @GetMapping(SearchConstants.USERS_SEARCH_PATH)
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> searchUsers(
            @RequestParam(name = SearchConstants.PARAM_Q, required = false) String q,
            @RequestParam(name = SearchConstants.PARAM_QUERY, required = false) String query,
            @PageableDefault(size = SearchConstants.DEFAULT_SEARCH_LIMIT) Pageable pageable) {
        String searchTerm = (q != null && !q.isBlank()) ? q : (query != null ? query : SearchConstants.DEFAULT_EMPTY_QUERY);
        PageResponse<UserSummaryResponse> response = searchService.searchUsers(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_USER_SEARCH_RESULTS, response));
    }

    @RateLimit(category = SearchConstants.RATE_LIMIT_SEARCH, limit = 40, windowSeconds = 60)
    @GetMapping(SearchConstants.POSTS_SEARCH_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> searchPosts(
            @RequestParam(name = SearchConstants.PARAM_Q, required = false) String q,
            @RequestParam(name = SearchConstants.PARAM_QUERY, required = false) String query,
            @PageableDefault(size = SearchConstants.DEFAULT_SEARCH_LIMIT, sort = SearchConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        String searchTerm = (q != null && !q.isBlank()) ? q : (query != null ? query : SearchConstants.DEFAULT_EMPTY_QUERY);
        PageResponse<PostResponse> response = searchService.searchPosts(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_POST_SEARCH_RESULTS, response));
    }

    @GetMapping(SearchConstants.EXPLORE_PATH)
    public ResponseEntity<ApiResponse<PageResponse<ExploreItemResponse>>> getExploreFeed(
            @PageableDefault(size = SearchConstants.DEFAULT_SEARCH_LIMIT) Pageable pageable) {
        PageResponse<ExploreItemResponse> response = searchService.getExploreFeed(pageable);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_EXPLORE_RETRIEVED, response));
    }

    @GetMapping(SearchConstants.TAGS_SEARCH_PATH)
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> searchHashtags(
            @RequestParam(name = SearchConstants.PARAM_Q, required = false) String q,
            @RequestParam(name = SearchConstants.PARAM_QUERY, required = false) String query) {
        String searchTerm = (q != null && !q.isBlank()) ? q : (query != null ? query : SearchConstants.DEFAULT_EMPTY_QUERY);
        List<HashtagResponse> response = searchService.searchHashtags(searchTerm);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_HASHTAGS_RETRIEVED, response));
    }

    @GetMapping(SearchConstants.TRENDING_TAGS_PATH)
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getTrendingHashtags() {
        List<HashtagResponse> response = searchService.getTrendingHashtags();
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_HASHTAGS_RETRIEVED, response));
    }

    @GetMapping(SearchConstants.TAG_POSTS_PATH)
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPostsByHashtag(
            @PathVariable String tag,
            @PageableDefault(size = SearchConstants.DEFAULT_SEARCH_LIMIT, sort = SearchConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PostResponse> response = searchService.getPostsByHashtag(tag, pageable);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_POST_SEARCH_RESULTS, response));
    }

    @GetMapping(SearchConstants.RECENT_SEARCH_PATH)
    public ResponseEntity<ApiResponse<List<RecentSearchResponse>>> getRecentSearches() {
        List<RecentSearchResponse> response = searchService.getRecentSearches();
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_RECENT_SEARCHES_RETRIEVED, response));
    }

    @PostMapping(SearchConstants.RECENT_SEARCH_PATH)
    public ResponseEntity<ApiResponse<RecentSearchResponse>> recordSearch(
            @Valid @RequestBody AddSearchHistoryRequest request) {
        RecentSearchResponse response = searchService.recordSearch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(SearchConstants.MSG_SEARCH_RECORDED, response));
    }

    @DeleteMapping(SearchConstants.RECENT_SEARCH_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteRecentSearch(@PathVariable UUID id) {
        searchService.deleteRecentSearch(id);
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_SEARCH_CLEARED, null));
    }

    @DeleteMapping(SearchConstants.RECENT_SEARCH_PATH)
    public ResponseEntity<ApiResponse<Void>> clearAllRecentSearches() {
        searchService.clearAllRecentSearches();
        return ResponseEntity.ok(ApiResponse.success(SearchConstants.MSG_SEARCH_CLEARED, null));
    }
}
