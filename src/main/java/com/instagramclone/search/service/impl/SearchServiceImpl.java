package com.instagramclone.search.service.impl;

import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.engagement.service.interfaces.IEngagementService;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.post.dto.response.PostMediaResponse;
import com.instagramclone.post.dto.response.PostResponse;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.entity.PostMedia;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.enums.ReelStatus;
import com.instagramclone.reel.repository.ReelRepository;
import com.instagramclone.reel.service.interfaces.IReelEngagementService;
import com.instagramclone.search.dto.request.AddSearchHistoryRequest;
import com.instagramclone.search.dto.response.ExploreItemResponse;
import com.instagramclone.search.dto.response.HashtagResponse;
import com.instagramclone.search.dto.response.RecentSearchResponse;
import com.instagramclone.search.entity.SearchHistory;
import com.instagramclone.search.enums.SearchType;
import com.instagramclone.search.repository.SearchHistoryRepository;
import com.instagramclone.search.service.interfaces.ISearchService;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.mapper.UserMapper;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserMapper userMapper;
    private final IEngagementService engagementService;
    private final IReelEngagementService reelEngagementService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable) {
        String cleanQuery = (query != null) ? query.trim() : "";
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        Page<User> usersPage = userRepository.searchUsers(cleanQuery, currentUserId, pageable);
        return PageResponse.from(usersPage.map(userMapper::toUserSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchPosts(String query, Pageable pageable) {
        String cleanQuery = (query != null) ? query.trim() : "";
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        Page<Post> postsPage = postRepository.searchPosts(cleanQuery, PostVisibility.PUBLIC, currentUserId, pageable);
        return PageResponse.from(postsPage.map(p -> toPostResponse(p, currentUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExploreItemResponse> getExploreFeed(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);

        Page<Post> postsPage = postRepository.findExplorePosts(PostVisibility.PUBLIC, currentUserId, pageable);
        Page<Reel> reelsPage = reelRepository.findFeedReels(ReelStatus.READY, currentUserId, pageable);

        List<ExploreItemResponse> items = new ArrayList<>();

        for (Post post : postsPage.getContent()) {
            String mediaUrl = (post.getMediaList() != null && !post.getMediaList().isEmpty())
                    ? post.getMediaList().get(0).getMediaUrl()
                    : null;
            long likes = engagementService.getLikeCount(post.getId());
            long comments = engagementService.getCommentCount(post.getId());

            User u = post.getUser();
            items.add(ExploreItemResponse.builder()
                    .id(post.getId())
                    .type("POST")
                    .mediaUrl(mediaUrl)
                    .thumbnailUrl(mediaUrl)
                    .caption(post.getCaption())
                    .likeCount(likes)
                    .commentCount(comments)
                    .author(PostAuthorResponse.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                            .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                            .build())
                    .createdAt(post.getCreatedAt())
                    .build());
        }

        for (Reel reel : reelsPage.getContent()) {
            long likes = reelEngagementService.getLikeCount(reel.getId());
            long comments = reelEngagementService.getCommentCount(reel.getId());

            User u = reel.getUser();
            items.add(ExploreItemResponse.builder()
                    .id(reel.getId())
                    .type("REEL")
                    .mediaUrl(reel.getVideoUrl())
                    .thumbnailUrl(reel.getThumbnailUrl())
                    .caption(reel.getCaption())
                    .likeCount(likes)
                    .commentCount(comments)
                    .author(PostAuthorResponse.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                            .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                            .build())
                    .createdAt(reel.getCreatedAt())
                    .build());
        }

        // Rank by engagement score: (likes * 2 + comments * 3)
        items.sort(Comparator.comparingLong((ExploreItemResponse item) -> (item.getLikeCount() * 2) + (item.getCommentCount() * 3)).reversed());

        long totalElements = postsPage.getTotalElements() + reelsPage.getTotalElements();
        Page<ExploreItemResponse> combinedPage = new PageImpl<>(items, pageable, totalElements);
        return PageResponse.from(combinedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponse> searchHashtags(String query) {
        String cleanQuery = (query != null) ? query.trim().replace("#", "").toLowerCase() : "";
        if (cleanQuery.isEmpty()) {
            return getTrendingHashtags();
        }

        Page<Post> sample = postRepository.searchPosts("#" + cleanQuery, PostVisibility.PUBLIC, null, PageRequest.of(0, 50));
        long count = sample.getTotalElements();

        List<HashtagResponse> result = new ArrayList<>();
        result.add(HashtagResponse.builder().name(cleanQuery).postCount(count).build());

        // Also check related tags in sample captions
        Map<String, Long> tagCounts = new HashMap<>();
        for (Post p : sample.getContent()) {
            if (p.getCaption() != null) {
                Matcher matcher = HASHTAG_PATTERN.matcher(p.getCaption());
                while (matcher.find()) {
                    String tag = matcher.group(1).toLowerCase();
                    if (tag.contains(cleanQuery) && !tag.equals(cleanQuery)) {
                        tagCounts.put(tag, tagCounts.getOrDefault(tag, 0L) + 1);
                    }
                }
            }
        }

        tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(9)
                .forEach(e -> result.add(HashtagResponse.builder().name(e.getKey()).postCount(e.getValue()).build()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponse> getTrendingHashtags() {
        Page<Post> recentPosts = postRepository.findExplorePosts(PostVisibility.PUBLIC, null, PageRequest.of(0, 50));
        Map<String, Long> tagCounts = new HashMap<>();

        for (Post p : recentPosts.getContent()) {
            if (p.getCaption() != null) {
                Matcher matcher = HASHTAG_PATTERN.matcher(p.getCaption());
                while (matcher.find()) {
                    String tag = matcher.group(1).toLowerCase();
                    tagCounts.put(tag, tagCounts.getOrDefault(tag, 0L) + 1);
                }
            }
        }

        if (tagCounts.isEmpty()) {
            // Default popular tags
            return List.of(
                    HashtagResponse.builder().name("photography").postCount(120).build(),
                    HashtagResponse.builder().name("nature").postCount(95).build(),
                    HashtagResponse.builder().name("explore").postCount(84).build(),
                    HashtagResponse.builder().name("travel").postCount(72).build(),
                    HashtagResponse.builder().name("instagram").postCount(65).build()
            );
        }

        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> HashtagResponse.builder().name(e.getKey()).postCount(e.getValue()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByHashtag(String tag, Pageable pageable) {
        String cleanTag = (tag != null) ? tag.trim().replace("#", "") : "";
        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(com.instagramclone.security.UserPrincipal::getId).orElse(null);
        Page<Post> postsPage = postRepository.searchPosts("#" + cleanTag, PostVisibility.PUBLIC, currentUserId, pageable);
        return PageResponse.from(postsPage.map(p -> toPostResponse(p, currentUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentSearchResponse> getRecentSearches() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<SearchHistory> list = searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, PageRequest.of(0, 20));

        return list.stream().map(sh -> {
            User target = sh.getTargetUser();
            UserSummaryResponse targetSummary = null;
            if (target != null) {
                targetSummary = UserSummaryResponse.builder()
                        .id(target.getId())
                        .username(target.getUsername())
                        .displayName(target.getProfile() != null ? target.getProfile().getDisplayName() : null)
                        .avatarUrl(target.getProfile() != null ? target.getProfile().getAvatarUrl() : null)
                        .build();
            }

            return RecentSearchResponse.builder()
                    .id(sh.getId())
                    .query(sh.getQuery())
                    .searchType(sh.getSearchType())
                    .targetUser(targetSummary)
                    .createdAt(sh.getCreatedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public RecentSearchResponse recordSearch(AddSearchHistoryRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        User targetUser = (request.getTargetUserId() != null)
                ? userRepository.findById(request.getTargetUserId()).orElse(null)
                : null;

        SearchHistory history = SearchHistory.builder()
                .user(currentUser)
                .query(request.getQuery() != null ? request.getQuery().trim() : null)
                .targetUser(targetUser)
                .searchType(request.getSearchType() != null ? request.getSearchType() : SearchType.KEYWORD)
                .build();

        SearchHistory saved = searchHistoryRepository.save(history);

        UserSummaryResponse targetSummary = null;
        if (targetUser != null) {
            targetSummary = UserSummaryResponse.builder()
                    .id(targetUser.getId())
                    .username(targetUser.getUsername())
                    .displayName(targetUser.getProfile() != null ? targetUser.getProfile().getDisplayName() : null)
                    .avatarUrl(targetUser.getProfile() != null ? targetUser.getProfile().getAvatarUrl() : null)
                    .build();
        }

        return RecentSearchResponse.builder()
                .id(saved.getId())
                .query(saved.getQuery())
                .searchType(saved.getSearchType())
                .targetUser(targetSummary)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteRecentSearch(UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        searchHistoryRepository.deleteByIdAndUserId(id, currentUserId);
    }

    @Override
    @Transactional
    public void clearAllRecentSearches() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        searchHistoryRepository.deleteByUserId(currentUserId);
    }

    private PostResponse toPostResponse(Post post, UUID viewerId) {
        User u = post.getUser();
        List<PostMediaResponse> mediaResponses = new ArrayList<>();
        if (post.getMediaList() != null) {
            for (PostMedia pm : post.getMediaList()) {
                mediaResponses.add(PostMediaResponse.builder()
                        .id(pm.getId())
                        .mediaUrl(pm.getMediaUrl())
                        .mediaType(pm.getMediaType())
                        .displayOrder(pm.getDisplayOrder())
                        .width(pm.getWidth())
                        .height(pm.getHeight())
                        .build());
            }
        }

        long likeCount = engagementService.getLikeCount(post.getId());
        long commentCount = engagementService.getCommentCount(post.getId());
        boolean liked = viewerId != null && engagementService.isLikedByUser(post.getId(), viewerId);
        boolean saved = viewerId != null && engagementService.isSavedByUser(post.getId(), viewerId);

        return PostResponse.builder()
                .id(post.getId())
                .author(PostAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .caption(post.getCaption())
                .visibility(post.getVisibility())
                .media(mediaResponses)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByCurrentUser(liked)
                .savedByCurrentUser(saved)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
