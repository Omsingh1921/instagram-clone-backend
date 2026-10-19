package com.instagramclone.search.service.interfaces;

import com.instagramclone.common.response.PageResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.search.dto.request.AddSearchHistoryRequest;
import com.instagramclone.search.dto.response.ExploreItemResponse;
import com.instagramclone.search.dto.response.HashtagResponse;
import com.instagramclone.search.dto.response.RecentSearchResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ISearchService {

    PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable);

    PageResponse<PostResponse> searchPosts(String query, Pageable pageable);

    PageResponse<ExploreItemResponse> getExploreFeed(Pageable pageable);

    List<HashtagResponse> searchHashtags(String query);

    List<HashtagResponse> getTrendingHashtags();

    PageResponse<PostResponse> getPostsByHashtag(String tag, Pageable pageable);

    List<RecentSearchResponse> getRecentSearches();

    RecentSearchResponse recordSearch(AddSearchHistoryRequest request);

    void deleteRecentSearch(UUID id);

    void clearAllRecentSearches();
}
