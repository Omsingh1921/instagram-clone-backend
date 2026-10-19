package com.instagramclone.story.service.impl;

import com.instagramclone.chat.dto.request.SendMessageRequest;
import com.instagramclone.chat.dto.response.ConversationResponse;
import com.instagramclone.chat.dto.response.MessageResponse;
import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.chat.service.interfaces.IChatService;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.service.interfaces.IFollowService;
import com.instagramclone.media.service.interfaces.IMediaService;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.post.dto.response.PostAuthorResponse;
import com.instagramclone.security.IPrivacyService;
import com.instagramclone.story.constants.StoryConstants;
import com.instagramclone.story.dto.request.CreateHighlightRequest;
import com.instagramclone.story.dto.request.CreateStoryRequest;
import com.instagramclone.story.dto.request.StoryMentionRequest;
import com.instagramclone.story.dto.request.StoryPollVoteRequest;
import com.instagramclone.story.dto.request.StoryReplyRequest;
import com.instagramclone.story.dto.request.UpdateHighlightRequest;
import com.instagramclone.story.dto.response.HighlightDetailResponse;
import com.instagramclone.story.dto.response.HighlightSummaryResponse;
import com.instagramclone.story.dto.response.StoryLikeResponse;
import com.instagramclone.story.dto.response.StoryMentionResponse;
import com.instagramclone.story.dto.response.StoryPollResponse;
import com.instagramclone.story.dto.response.StoryResponse;
import com.instagramclone.story.dto.response.StoryViewerResponse;
import com.instagramclone.story.dto.response.UserStoriesResponse;
import com.instagramclone.story.entity.Story;
import com.instagramclone.story.entity.StoryHighlight;
import com.instagramclone.story.entity.StoryHighlightItem;
import com.instagramclone.story.entity.StoryLike;
import com.instagramclone.story.entity.StoryMention;
import com.instagramclone.story.entity.StoryPoll;
import com.instagramclone.story.entity.StoryPollVote;
import com.instagramclone.story.entity.StoryView;
import com.instagramclone.story.repository.StoryHighlightItemRepository;
import com.instagramclone.story.repository.StoryHighlightRepository;
import com.instagramclone.story.repository.StoryLikeRepository;
import com.instagramclone.story.repository.StoryMentionRepository;
import com.instagramclone.story.repository.StoryPollRepository;
import com.instagramclone.story.repository.StoryPollVoteRepository;
import com.instagramclone.story.repository.StoryRepository;
import com.instagramclone.story.repository.StoryViewRepository;
import com.instagramclone.story.service.interfaces.IStoryService;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.CloseFriendRepository;
import com.instagramclone.user.repository.ProfileRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.utility.RedisDistributedLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements IStoryService {

    private static final Logger log = LoggerFactory.getLogger(StoryServiceImpl.class);

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final StoryMentionRepository storyMentionRepository;
    private final StoryLikeRepository storyLikeRepository;
    private final StoryHighlightRepository storyHighlightRepository;
    private final StoryHighlightItemRepository storyHighlightItemRepository;
    private final StoryPollRepository storyPollRepository;
    private final StoryPollVoteRepository storyPollVoteRepository;
    private final CloseFriendRepository closeFriendRepository;
    private final UserRepository userRepository;
    private final IFollowService followService;
    private final ProfileRepository profileRepository;
    private final IMediaService mediaService;
    private final IPrivacyService privacyService;
    private final INotificationService notificationService;
    private final IChatService chatService;
    private final RedisDistributedLockService lockService;
    private final PlatformTransactionManager transactionManager;

    private boolean acquireLockWithRetry(String lockKey, String lockOwner, Duration lease, int maxRetries, long sleepMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (lockService.tryLock(lockKey, lockOwner, lease)) {
                return true;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public StoryResponse createStory(CreateStoryRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + currentUserId));

        Story story = Story.builder()
                .user(author)
                .mediaUrl(request.getMediaUrl().trim())
                .mediaType(request.getMediaType())
                .caption(request.getCaption() != null ? request.getCaption().trim() : null)
                .isCloseFriendsOnly(request.isCloseFriendsOnly())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        Story saved = storyRepository.save(story);
        log.info("Created story {} for user {} (closeFriendsOnly: {})", saved.getId(), author.getUsername(), saved.isCloseFriendsOnly());

        if (request.getPoll() != null && request.getPoll().getQuestion() != null && !request.getPoll().getQuestion().isBlank()) {
            StoryPoll poll = StoryPoll.builder()
                    .story(saved)
                    .question(request.getPoll().getQuestion().trim())
                    .optionA(request.getPoll().getOptionA() != null && !request.getPoll().getOptionA().isBlank() ? request.getPoll().getOptionA().trim() : "Yes")
                    .optionB(request.getPoll().getOptionB() != null && !request.getPoll().getOptionB().isBlank() ? request.getPoll().getOptionB().trim() : "No")
                    .build();
            storyPollRepository.save(poll);
            saved.setPoll(poll);
            log.info("Created interactive poll for story {}", saved.getId());
        }

        if (request.getMentions() != null && !request.getMentions().isEmpty()) {
            for (StoryMentionRequest mentionReq : request.getMentions()) {
                if (mentionReq.getUserId() == null || mentionReq.getUserId().equals(currentUserId)) {
                    continue;
                }
                userRepository.findById(mentionReq.getUserId()).ifPresent(mentionedUser -> {
                    if (mentionedUser.isDeletedOrDeactivated()) {
                        return;
                    }
                    if (privacyService.isBlockedBidirectional(currentUserId, mentionedUser.getId())) {
                        return;
                    }
                    StoryMention mention = StoryMention.builder()
                            .story(saved)
                            .mentionedUser(mentionedUser)
                            .xPercent(mentionReq.getXPercent())
                            .yPercent(mentionReq.getYPercent())
                            .build();
                    storyMentionRepository.save(mention);

                    notificationService.createNotification(
                            mentionedUser.getId(),
                            currentUserId,
                            NotificationType.USER_MENTIONED,
                            null,
                            null
                    );
                });
            }
        }

        return toStoryResponse(saved, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStoriesResponse> getStoryFeed() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<UUID> followingIds = new ArrayList<>(followService.getFollowingIds(currentUserId));
        followingIds.removeIf(fId -> privacyService.isBlockedBidirectional(currentUserId, fId));
        if (!followingIds.contains(currentUserId)) {
            followingIds.add(currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Story> activeStories = storyRepository.findActiveStoriesByUserIds(followingIds, now);

        // Group by user & filter close friends stories
        Map<UUID, List<Story>> userStoriesMap = new LinkedHashMap<>();
        for (Story story : activeStories) {
            if (story.isCloseFriendsOnly()) {
                boolean isAuthor = story.getUser().getId().equals(currentUserId);
                boolean isCloseFriend = !isAuthor && closeFriendRepository.existsByUserIdAndFriendId(story.getUser().getId(), currentUserId);
                if (!isAuthor && !isCloseFriend) {
                    continue; // Skip close friends story for non-close friends
                }
            }
            userStoriesMap.computeIfAbsent(story.getUser().getId(), k -> new ArrayList<>()).add(story);
        }

        List<UserStoriesResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Story>> entry : userStoriesMap.entrySet()) {
            List<Story> list = entry.getValue();
            if (list.isEmpty()) continue;

            User author = list.get(0).getUser();
            List<StoryResponse> storyResponses = list.stream()
                    .map(s -> toStoryResponse(s, currentUserId))
                    .toList();

            boolean allViewed = storyResponses.stream().allMatch(StoryResponse::isViewed);
            boolean hasCloseFriendsStories = storyResponses.stream().anyMatch(StoryResponse::isCloseFriendsOnly);

            result.add(UserStoriesResponse.builder()
                    .user(UserSummaryResponse.builder()
                            .id(author.getId())
                            .username(author.getUsername())
                            .displayName(author.getProfile() != null ? author.getProfile().getDisplayName() : null)
                            .avatarUrl(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                            .build())
                    .stories(storyResponses)
                    .allViewed(allViewed)
                    .hasCloseFriendsStories(hasCloseFriendsStories)
                    .build());
        }

        // Sort: unviewed stories first, except current user always at the very front
        result.sort(Comparator.comparing((UserStoriesResponse usr) -> !usr.getUser().getId().equals(currentUserId))
                .thenComparing(UserStoriesResponse::isAllViewed));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserStoriesResponse getUserStories(UUID userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_USER_NOT_FOUND + userId));

        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        if (!isAdmin && author.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_USER_NOT_FOUND + userId);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null);
        privacyService.validateCanViewUserContent(currentUserId, userId);

        LocalDateTime now = LocalDateTime.now();
        List<Story> activeStories = storyRepository.findActiveStoriesByUserId(userId, now);

        List<StoryResponse> storyResponses = activeStories.stream()
                .filter(story -> {
                    if (story.isCloseFriendsOnly()) {
                        if (currentUserId == null) return false;
                        boolean isAuthor = currentUserId.equals(userId);
                        boolean isCloseFriend = !isAuthor && closeFriendRepository.existsByUserIdAndFriendId(userId, currentUserId);
                        return isAuthor || isCloseFriend;
                    }
                    return true;
                })
                .map(s -> toStoryResponse(s, currentUserId))
                .toList();

        boolean allViewed = storyResponses.stream().allMatch(StoryResponse::isViewed);
        boolean hasCloseFriendsStories = storyResponses.stream().anyMatch(StoryResponse::isCloseFriendsOnly);

        return UserStoriesResponse.builder()
                .user(UserSummaryResponse.builder()
                        .id(author.getId())
                        .username(author.getUsername())
                        .displayName(author.getProfile() != null ? author.getProfile().getDisplayName() : null)
                        .avatarUrl(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                        .build())
                .stories(storyResponses)
                .allViewed(allViewed)
                .hasCloseFriendsStories(hasCloseFriendsStories)
                .build();
    }

    @Override
    public void markStoryViewed(UUID storyId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String lockKey = "lock:story:view:" + currentUserId + ":" + storyId;
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = acquireLockWithRetry(lockKey, lockOwner, Duration.ofSeconds(5), 20, 50);

        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.executeWithoutResult(status -> executeMarkStoryViewed(currentUserId, storyId));
        } finally {
            if (locked) {
                lockService.releaseLock(lockKey, lockOwner);
            }
        }
    }

    private void executeMarkStoryViewed(UUID currentUserId, UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        boolean isHighlighted = storyHighlightItemRepository.existsByStoryId(storyId);
        if ((story.getExpiresAt().isBefore(LocalDateTime.now()) && !isHighlighted) || story.getUser().isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
        }

        if (story.isCloseFriendsOnly() && !story.getUser().getId().equals(currentUserId)) {
            if (!closeFriendRepository.existsByUserIdAndFriendId(story.getUser().getId(), currentUserId)) {
                throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
            }
        }

        privacyService.validateCanViewUserContent(currentUserId, story.getUser().getId());

        if (!storyViewRepository.existsByStoryIdAndUserId(storyId, currentUserId)) {
            User viewer = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_USER_NOT_FOUND));

            StoryView view = StoryView.builder()
                    .story(story)
                    .user(viewer)
                    .build();
            try {
                storyViewRepository.save(view);
                log.info("User {} viewed story {}", currentUserId, storyId);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                log.debug("Concurrent view deduplicated for story {} by user {}", storyId, currentUserId);
            }
        }
    }

    @Override
    @Transactional
    public StoryLikeResponse toggleLikeStory(UUID storyId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        boolean isHighlighted = storyHighlightItemRepository.existsByStoryId(storyId);
        if ((story.getExpiresAt().isBefore(LocalDateTime.now()) && !isHighlighted) || story.getUser().isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
        }

        if (story.isCloseFriendsOnly() && !story.getUser().getId().equals(currentUserId)) {
            if (!closeFriendRepository.existsByUserIdAndFriendId(story.getUser().getId(), currentUserId)) {
                throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
            }
        }

        privacyService.validateCanViewUserContent(currentUserId, story.getUser().getId());

        Optional<StoryLike> existingLike = storyLikeRepository.findByStoryIdAndUserId(storyId, currentUserId);
        if (existingLike.isPresent()) {
            storyLikeRepository.delete(existingLike.get());
            storyLikeRepository.flush();
            long count = storyLikeRepository.countByStoryId(storyId);
            log.info("User {} unliked story {}", currentUserId, storyId);
            return StoryLikeResponse.builder()
                    .storyId(storyId)
                    .liked(false)
                    .likeCount(count)
                    .build();
        } else {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_USER_NOT_FOUND));

            StoryLike like = StoryLike.builder()
                    .story(story)
                    .user(currentUser)
                    .build();
            storyLikeRepository.save(like);
            storyLikeRepository.flush();
            long count = storyLikeRepository.countByStoryId(storyId);
            log.info("User {} liked story {}", currentUserId, storyId);

            if (!story.getUser().getId().equals(currentUserId)) {
                notificationService.createNotification(
                        story.getUser().getId(),
                        currentUserId,
                        NotificationType.STORY_LIKED,
                        null,
                        null
                );
            }

            return StoryLikeResponse.builder()
                    .storyId(storyId)
                    .liked(true)
                    .likeCount(count)
                    .build();
        }
    }

    @Override
    @Transactional
    public MessageResponse replyToStory(UUID storyId, StoryReplyRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        boolean isHighlighted = storyHighlightItemRepository.existsByStoryId(storyId);
        if ((story.getExpiresAt().isBefore(LocalDateTime.now()) && !isHighlighted) || story.getUser().isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
        }

        if (story.isCloseFriendsOnly() && !story.getUser().getId().equals(currentUserId)) {
            if (!closeFriendRepository.existsByUserIdAndFriendId(story.getUser().getId(), currentUserId)) {
                throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
            }
        }

        privacyService.validateCanViewUserContent(currentUserId, story.getUser().getId());

        if (story.getUser().getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot reply to your own story");
        }

        ConversationResponse conv = chatService.getOrCreateDirectConversation(story.getUser().getId());

        SendMessageRequest sendMsg = SendMessageRequest.builder()
                .content(request.getContent().trim())
                .mediaUrl(story.getMediaUrl())
                .messageType(MessageType.TEXT)
                .build();

        MessageResponse msgResponse = chatService.sendMessage(conv.getId(), sendMsg);
        log.info("User {} replied to story {} of user {}", currentUserId, storyId, story.getUser().getId());
        return msgResponse;
    }

    @Override
    @Transactional
    public StoryPollResponse voteOnPoll(UUID storyId, StoryPollVoteRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        boolean isHighlighted = storyHighlightItemRepository.existsByStoryId(storyId);
        if ((story.getExpiresAt().isBefore(LocalDateTime.now()) && !isHighlighted) || story.getUser().isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
        }

        if (story.isCloseFriendsOnly() && !story.getUser().getId().equals(currentUserId)) {
            if (!closeFriendRepository.existsByUserIdAndFriendId(story.getUser().getId(), currentUserId)) {
                throw new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId);
            }
        }

        privacyService.validateCanViewUserContent(currentUserId, story.getUser().getId());

        StoryPoll poll = storyPollRepository.findByStoryId(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_POLL_NOT_FOUND));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_USER_NOT_FOUND));

        Optional<StoryPollVote> existingVote = storyPollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUserId);
        if (existingVote.isPresent()) {
            StoryPollVote vote = existingVote.get();
            vote.setSelectedOption(request.getSelectedOption());
            storyPollVoteRepository.save(vote);
            log.info("User {} updated vote on poll {} to option {}", currentUserId, poll.getId(), request.getSelectedOption());
        } else {
            StoryPollVote vote = StoryPollVote.builder()
                    .poll(poll)
                    .user(currentUser)
                    .selectedOption(request.getSelectedOption())
                    .build();
            storyPollVoteRepository.save(vote);
            log.info("User {} voted on poll {} option {}", currentUserId, poll.getId(), request.getSelectedOption());
        }

        storyPollVoteRepository.flush();
        return toStoryPollResponse(poll, currentUserId);
    }

    @Override
    @Transactional
    public void deleteStory(UUID storyId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        if (!story.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(StoryConstants.ERR_UNAUTHORIZED_DELETE);
        }

        try {
            mediaService.deleteByUrl(story.getMediaUrl());
        } catch (Exception ex) {
            log.warn("Failed to delete story media asset from Cloudinary: {}", ex.getMessage());
        }

        storyPollVoteRepository.deleteByStoryId(storyId);
        storyPollRepository.deleteByStoryId(storyId);
        storyLikeRepository.deleteByStoryId(storyId);
        storyHighlightItemRepository.deleteByStoryId(storyId);
        storyMentionRepository.deleteByStoryId(storyId);
        storyViewRepository.deleteByStoryId(storyId);
        storyRepository.delete(story);
        log.info("Story {} deleted by user {}", storyId, currentUserId);
    }

    @Override
    @Transactional
    public int cleanupExpiredStories() {
        LocalDateTime now = LocalDateTime.now();
        List<Story> expiredStories = storyRepository.findExpiredStoriesNotInHighlights(now);
        for (Story expired : expiredStories) {
            try {
                mediaService.deleteByUrl(expired.getMediaUrl());
            } catch (Exception ex) {
                log.warn("Failed to clean up expired story media asset: {}", ex.getMessage());
            }
            storyPollVoteRepository.deleteByStoryId(expired.getId());
            storyPollRepository.deleteByStoryId(expired.getId());
            storyLikeRepository.deleteByStoryId(expired.getId());
            storyMentionRepository.deleteByStoryId(expired.getId());
            storyViewRepository.deleteByStoryId(expired.getId());
            storyRepository.delete(expired);
        }
        log.info("Cleaned up {} expired stories at {}", expiredStories.size(), now);
        return expiredStories.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryViewerResponse> getStoryViewers(UUID storyId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_STORY_NOT_FOUND + storyId));

        if (!story.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(StoryConstants.ERR_UNAUTHORIZED_VIEWERS);
        }

        List<StoryView> views = storyViewRepository.findByStoryIdWithUser(storyId);
        List<StoryLike> likes = storyLikeRepository.findByStoryId(storyId);
        Set<UUID> likedUserIds = likes.stream()
                .map(l -> l.getUser().getId())
                .collect(Collectors.toSet());

        return views.stream().map(v -> {
            User u = v.getUser();
            return StoryViewerResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .displayName(u.getProfile() != null && u.getProfile().getDisplayName() != null ? u.getProfile().getDisplayName() : u.getUsername())
                    .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                    .viewedAt(v.getViewedAt())
                    .hasLiked(likedUserIds.contains(u.getId()))
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public HighlightSummaryResponse createHighlight(CreateHighlightRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_USER_NOT_FOUND));

        if (request.getStoryIds() == null || request.getStoryIds().isEmpty()) {
            throw new BadRequestException(StoryConstants.ERR_HIGHLIGHT_STORIES_EMPTY);
        }

        List<Story> stories = storyRepository.findAllById(request.getStoryIds());
        Map<UUID, Story> storyMap = stories.stream().collect(Collectors.toMap(Story::getId, s -> s));

        List<Story> orderedStories = new ArrayList<>();
        for (UUID sid : request.getStoryIds()) {
            Story s = storyMap.get(sid);
            if (s != null && s.getUser().getId().equals(currentUserId) && !orderedStories.contains(s)) {
                orderedStories.add(s);
            }
        }

        if (orderedStories.isEmpty()) {
            throw new BadRequestException(StoryConstants.ERR_HIGHLIGHT_STORIES_EMPTY);
        }

        String coverUrl = (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isBlank())
                ? request.getCoverImageUrl().trim()
                : orderedStories.get(0).getMediaUrl();

        StoryHighlight highlight = StoryHighlight.builder()
                .user(currentUser)
                .title(request.getTitle().trim())
                .coverImageUrl(coverUrl)
                .build();
        StoryHighlight savedHighlight = storyHighlightRepository.save(highlight);

        List<StoryHighlightItem> items = new ArrayList<>();
        for (int i = 0; i < orderedStories.size(); i++) {
            items.add(StoryHighlightItem.builder()
                    .highlight(savedHighlight)
                    .story(orderedStories.get(i))
                    .orderIndex(i)
                    .build());
        }
        storyHighlightItemRepository.saveAll(items);
        savedHighlight.setItems(items);

        log.info("Created highlight {} with {} stories for user {}", savedHighlight.getId(), items.size(), currentUserId);

        return HighlightSummaryResponse.builder()
                .id(savedHighlight.getId())
                .title(savedHighlight.getTitle())
                .coverImageUrl(savedHighlight.getCoverImageUrl())
                .storyCount(items.size())
                .createdAt(savedHighlight.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HighlightDetailResponse getHighlightById(UUID highlightId) {
        StoryHighlight highlight = storyHighlightRepository.findByIdWithItems(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_HIGHLIGHT_NOT_FOUND + highlightId));

        if (highlight.getUser().isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_HIGHLIGHT_NOT_FOUND + highlightId);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null);
        privacyService.validateCanViewUserContent(currentUserId, highlight.getUser().getId());

        List<StoryHighlightItem> items = storyHighlightItemRepository.findByHighlightIdOrderByOrderIndexAsc(highlightId);
        List<StoryResponse> storyResponses = items.stream()
                .map(item -> toStoryResponse(item.getStory(), currentUserId))
                .toList();

        User author = highlight.getUser();
        PostAuthorResponse authorResponse = PostAuthorResponse.builder()
                .id(author.getId())
                .username(author.getUsername())
                .displayName(author.getProfile() != null ? author.getProfile().getDisplayName() : null)
                .avatarUrl(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                .build();

        return HighlightDetailResponse.builder()
                .id(highlight.getId())
                .title(highlight.getTitle())
                .coverImageUrl(highlight.getCoverImageUrl())
                .author(authorResponse)
                .stories(storyResponses)
                .createdAt(highlight.getCreatedAt())
                .updatedAt(highlight.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HighlightSummaryResponse> getUserHighlights(UUID userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_USER_NOT_FOUND + userId));

        if (author.isDeletedOrDeactivated() && !SecurityUtils.isCurrentUserAdmin()) {
            throw new ResourceNotFoundException(StoryConstants.ERR_USER_NOT_FOUND + userId);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null);
        privacyService.validateCanViewUserContent(currentUserId, userId);

        List<StoryHighlight> highlights = storyHighlightRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return highlights.stream().map(h -> HighlightSummaryResponse.builder()
                .id(h.getId())
                .title(h.getTitle())
                .coverImageUrl(h.getCoverImageUrl())
                .storyCount(h.getItems() != null ? h.getItems().size() : 0)
                .createdAt(h.getCreatedAt())
                .build()).toList();
    }

    @Override
    @Transactional
    public HighlightSummaryResponse updateHighlight(UUID highlightId, UpdateHighlightRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        StoryHighlight highlight = storyHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_HIGHLIGHT_NOT_FOUND + highlightId));

        if (!highlight.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(StoryConstants.ERR_UNAUTHORIZED_HIGHLIGHT);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            highlight.setTitle(request.getTitle().trim());
        }

        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isBlank()) {
            highlight.setCoverImageUrl(request.getCoverImageUrl().trim());
        }

        if (request.getStoryIds() != null) {
            if (request.getStoryIds().isEmpty()) {
                throw new BadRequestException(StoryConstants.ERR_HIGHLIGHT_STORIES_EMPTY);
            }
            List<Story> stories = storyRepository.findAllById(request.getStoryIds());
            Map<UUID, Story> storyMap = stories.stream().collect(Collectors.toMap(Story::getId, s -> s));

            List<Story> orderedStories = new ArrayList<>();
            for (UUID sid : request.getStoryIds()) {
                Story s = storyMap.get(sid);
                if (s != null && s.getUser().getId().equals(currentUserId) && !orderedStories.contains(s)) {
                    orderedStories.add(s);
                }
            }

            if (orderedStories.isEmpty()) {
                throw new BadRequestException(StoryConstants.ERR_HIGHLIGHT_STORIES_EMPTY);
            }

            storyHighlightItemRepository.deleteByHighlightId(highlightId);
            storyHighlightItemRepository.flush();

            List<StoryHighlightItem> newItems = new ArrayList<>();
            for (int i = 0; i < orderedStories.size(); i++) {
                newItems.add(StoryHighlightItem.builder()
                        .highlight(highlight)
                        .story(orderedStories.get(i))
                        .orderIndex(i)
                        .build());
            }
            storyHighlightItemRepository.saveAll(newItems);
            highlight.setItems(newItems);
        }

        StoryHighlight updated = storyHighlightRepository.save(highlight);
        log.info("Highlight {} updated by user {}", highlightId, currentUserId);

        return HighlightSummaryResponse.builder()
                .id(updated.getId())
                .title(updated.getTitle())
                .coverImageUrl(updated.getCoverImageUrl())
                .storyCount(updated.getItems() != null ? updated.getItems().size() : 0)
                .createdAt(updated.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteHighlight(UUID highlightId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        StoryHighlight highlight = storyHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new ResourceNotFoundException(StoryConstants.ERR_HIGHLIGHT_NOT_FOUND + highlightId));

        if (!highlight.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(StoryConstants.ERR_UNAUTHORIZED_HIGHLIGHT);
        }

        storyHighlightRepository.delete(highlight);
        log.info("Highlight {} deleted by user {}", highlightId, currentUserId);
    }

    private StoryPollResponse toStoryPollResponse(StoryPoll poll, UUID viewerId) {
        if (poll == null) return null;
        long voteCountA = storyPollVoteRepository.countByPollIdAndSelectedOption(poll.getId(), 1);
        long voteCountB = storyPollVoteRepository.countByPollIdAndSelectedOption(poll.getId(), 2);
        long total = voteCountA + voteCountB;
        double percentA = total > 0 ? Math.round(((double) voteCountA / total) * 1000.0) / 10.0 : 0.0;
        double percentB = total > 0 ? Math.round(((double) voteCountB / total) * 1000.0) / 10.0 : 0.0;
        Integer userVote = viewerId != null
                ? storyPollVoteRepository.findByPollIdAndUserId(poll.getId(), viewerId).map(StoryPollVote::getSelectedOption).orElse(null)
                : null;

        return StoryPollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .optionA(poll.getOptionA())
                .optionB(poll.getOptionB())
                .voteCountA(voteCountA)
                .voteCountB(voteCountB)
                .totalVotes(total)
                .percentA(percentA)
                .percentB(percentB)
                .userVote(userVote)
                .build();
    }

    private StoryResponse toStoryResponse(Story story, UUID viewerId) {
        User u = story.getUser();
        boolean viewed = viewerId != null && storyViewRepository.existsByStoryIdAndUserId(story.getId(), viewerId);
        long viewCount = storyViewRepository.countByStoryId(story.getId());
        boolean liked = viewerId != null && storyLikeRepository.existsByStoryIdAndUserId(story.getId(), viewerId);
        long likeCount = storyLikeRepository.countByStoryId(story.getId());

        List<StoryMentionResponse> mentionResponses =
                storyMentionRepository.findByStoryIdWithUser(story.getId()).stream().map(m -> {
                    User mu = m.getMentionedUser();
                    return StoryMentionResponse.builder()
                            .userId(mu.getId())
                            .username(mu.getUsername())
                            .displayName(mu.getProfile() != null && mu.getProfile().getDisplayName() != null ? mu.getProfile().getDisplayName() : mu.getUsername())
                            .avatarUrl(mu.getProfile() != null ? mu.getProfile().getAvatarUrl() : null)
                            .xPercent(m.getXPercent())
                            .yPercent(m.getYPercent())
                            .build();
                }).toList();

        StoryPoll poll = story.getPoll() != null ? story.getPoll() : storyPollRepository.findByStoryId(story.getId()).orElse(null);
        StoryPollResponse pollResponse = toStoryPollResponse(poll, viewerId);

        return StoryResponse.builder()
                .id(story.getId())
                .author(PostAuthorResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getProfile() != null ? u.getProfile().getDisplayName() : null)
                        .avatarUrl(u.getProfile() != null ? u.getProfile().getAvatarUrl() : null)
                        .build())
                .mediaUrl(story.getMediaUrl())
                .mediaType(story.getMediaType())
                .caption(story.getCaption())
                .closeFriendsOnly(story.isCloseFriendsOnly())
                .poll(pollResponse)
                .mentions(mentionResponses)
                .viewed(viewed)
                .viewCount(viewCount)
                .liked(liked)
                .likeCount(likeCount)
                .expiresAt(story.getExpiresAt())
                .createdAt(story.getCreatedAt())
                .build();
    }
}
