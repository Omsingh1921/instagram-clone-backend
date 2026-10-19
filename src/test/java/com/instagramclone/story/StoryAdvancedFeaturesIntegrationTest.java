package com.instagramclone.story;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.follow.entity.Follow;
import com.instagramclone.notification.entity.Notification;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.story.dto.request.CreateHighlightRequest;
import com.instagramclone.story.dto.request.CreateStoryPollRequest;
import com.instagramclone.story.dto.request.CreateStoryRequest;
import com.instagramclone.story.dto.request.StoryMentionRequest;
import com.instagramclone.story.dto.request.StoryPollVoteRequest;
import com.instagramclone.story.dto.request.StoryReplyRequest;
import com.instagramclone.story.dto.request.UpdateHighlightRequest;
import com.instagramclone.story.dto.response.StoryResponse;
import com.instagramclone.story.entity.Story;
import com.instagramclone.story.service.interfaces.IStoryService;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoryAdvancedFeaturesIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IStoryService storyService;

    private User creator;
    private User mentionedUser;
    private User viewer;
    private User blockedUser;

    private String creatorToken;
    private String viewerToken;
    private String mentionedUserToken;
    private String blockedUserToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        creator = userRepository.save(User.builder()
                .username("story_creator")
                .email("creator@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        profileRepository.save(Profile.builder()
                .user(creator)
                .displayName("Story Creator Pro")
                .avatarUrl("https://cloudinary.com/avatar/creator.jpg")
                .build());

        mentionedUser = userRepository.save(User.builder()
                .username("mentioned_friend")
                .email("mentioned@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        profileRepository.save(Profile.builder()
                .user(mentionedUser)
                .displayName("Mentioned Friend")
                .avatarUrl("https://cloudinary.com/avatar/friend.jpg")
                .build());

        viewer = userRepository.save(User.builder()
                .username("story_viewer")
                .email("viewer@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        profileRepository.save(Profile.builder()
                .user(viewer)
                .displayName("Active Viewer")
                .avatarUrl("https://cloudinary.com/avatar/viewer.jpg")
                .build());

        blockedUser = userRepository.save(User.builder()
                .username("blocked_hater")
                .email("blocked@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(creator)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(mentionedUser)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(viewer)
                .isPrivate(false)
                .build());

        // viewer follows creator
        followRepository.save(Follow.builder()
                .follower(viewer)
                .following(creator)
                .build());

        followRepository.save(Follow.builder()
                .follower(mentionedUser)
                .following(creator)
                .build());

        // creator blocks blockedUser
        userBlockRepository.save(UserBlock.builder()
                .blocker(creator)
                .blocked(blockedUser)
                .build());

        creatorToken = jwtTokenProvider.generateTokenForUser(creator.getId(), creator.getUsername(), creator.getEmail());
        viewerToken = jwtTokenProvider.generateTokenForUser(viewer.getId(), viewer.getUsername(), viewer.getEmail());
        mentionedUserToken = jwtTokenProvider.generateTokenForUser(mentionedUser.getId(), mentionedUser.getUsername(), mentionedUser.getEmail());
        blockedUserToken = jwtTokenProvider.generateTokenForUser(blockedUser.getId(), blockedUser.getUsername(), blockedUser.getEmail());
    }

    @Test
    @DisplayName("Story Mentions - Create story with mentions, coordinates, and verify notification dispatch")
    void createStoryWithMentions_ShouldPersistAndNotify() throws Exception {
        CreateStoryRequest request = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/story_with_mention.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Celebrating with my friend! 🎉")
                .mentions(List.of(
                        StoryMentionRequest.builder()
                                .userId(mentionedUser.getId())
                                .xPercent(45.5)
                                .yPercent(70.2)
                                .build()
                ))
                .build();

        String response = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.caption").value("Celebrating with my friend! 🎉"))
                .andExpect(jsonPath("$.data.mentions[0].userId").value(mentionedUser.getId().toString()))
                .andExpect(jsonPath("$.data.mentions[0].username").value("mentioned_friend"))
                .andExpect(jsonPath("$.data.mentions[0].displayName").value("Mentioned Friend"))
                .andExpect(jsonPath("$.data.mentions[0].xPercent").value(45.5))
                .andExpect(jsonPath("$.data.mentions[0].yPercent").value(70.2))
                .andReturn().getResponse().getContentAsString();

        // Verify USER_MENTIONED notification created for mentioned friend
        List<Notification> notifs = notificationRepository.findAll();
        boolean hasMentionNotification = notifs.stream()
                .anyMatch(n -> n.getRecipient().getId().equals(mentionedUser.getId())
                        && n.getActor().getId().equals(creator.getId())
                        && n.getType() == NotificationType.USER_MENTIONED);

        assertThat(hasMentionNotification).isTrue();
    }

    @Test
    @DisplayName("Story Feed & Viewers - Feed returns active stories and viewers analytics enforces IDOR security")
    void storyFeedAndViewers_ShouldWorkCorrectlyWithIDORProtection() throws Exception {
        // 1. Creator posts story
        CreateStoryRequest request = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/feed_story.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Day at the beach")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID storyId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 2. Viewer fetches feed -> must contain creator's story
        mockMvc.perform(get("/api/v1/stories/feed")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.user.username == 'story_creator')].stories[0].caption").value("Day at the beach"));

        // 3. Viewer marks story as viewed
        mockMvc.perform(post("/api/v1/stories/{storyId}/view", storyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        // 4. Creator inspects who viewed their story -> 200 OK with viewer profile details
        mockMvc.perform(get("/api/v1/stories/{storyId}/viewers", storyId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("story_viewer"))
                .andExpect(jsonPath("$.data[0].displayName").value("Active Viewer"));

        // 5. Stranger viewer attempts to inspect creator's story viewers -> 403 Forbidden (IDOR Guard)
        mockMvc.perform(get("/api/v1/stories/{storyId}/viewers", storyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Concurrent Story Views - Duplicate playback requests gracefully deduplicate without 500 error")
    void concurrentStoryViews_ShouldDeduplicateGracefully() throws Exception {
        // Creator posts story
        CreateStoryRequest request = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/concurrency_story.jpg")
                .mediaType(MediaType.IMAGE)
                .build();

        String createRes = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID storyId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);
        List<Integer> statuses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int st = mockMvc.perform(post("/api/v1/stories/{storyId}/view", storyId)
                                    .header("Authorization", "Bearer " + viewerToken))
                            .andReturn().getResponse().getStatus();
                    statuses.add(st);
                } catch (Exception ex) {
                    statuses.add(500);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // All concurrent requests must return 200 OK (no 500 DataIntegrityViolationException)
        assertThat(statuses).hasSize(threads);
        assertThat(statuses).allMatch(s -> s == 200);

        executor.shutdown();
    }

    @Test
    @DisplayName("Story Likes - Toggle heart reaction, verify like count, notification, and viewers flag")
    void storyLikes_ToggleHeartAndShowInViewersList() throws Exception {
        // 1. Creator posts story
        CreateStoryRequest request = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/heart_story.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Sunset vibes 🌅")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID storyId = UUID.fromString(objectMapper.readTree(createRes).get("data").get("id").asText());

        // 2. Viewer views the story
        mockMvc.perform(post("/api/v1/stories/{storyId}/view", storyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        // 3. Creator views viewers -> viewer hasLiked = false
        mockMvc.perform(get("/api/v1/stories/{storyId}/viewers", storyId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("story_viewer"))
                .andExpect(jsonPath("$.data[0].hasLiked").value(false));

        // 4. Viewer sends heart reaction (likes story)
        mockMvc.perform(post("/api/v1/stories/{storyId}/like", storyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storyId").value(storyId.toString()))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // 5. Verify notification created for creator
        List<Notification> notifs = notificationRepository.findAll();
        boolean hasLikeNotification = notifs.stream()
                .anyMatch(n -> n.getRecipient().getId().equals(creator.getId())
                        && n.getActor().getId().equals(viewer.getId())
                        && n.getType() == NotificationType.STORY_LIKED);
        assertThat(hasLikeNotification).isTrue();

        // 6. Creator views viewers -> viewer hasLiked = true (Heart ❤️ indicator)
        mockMvc.perform(get("/api/v1/stories/{storyId}/viewers", storyId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("story_viewer"))
                .andExpect(jsonPath("$.data[0].hasLiked").value(true));

        // 7. Viewer unlikes story (toggle)
        mockMvc.perform(post("/api/v1/stories/{storyId}/like", storyId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        // 8. Creator views viewers -> viewer hasLiked = false
        mockMvc.perform(get("/api/v1/stories/{storyId}/viewers", storyId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].hasLiked").value(false));
    }

    @Test
    @DisplayName("Story Highlights - Full lifecycle: create highlight, fetch details, update, IDOR protection, and delete")
    void storyHighlights_FullLifecycleAndAuthorization() throws Exception {
        // 1. Creator posts two stories
        CreateStoryRequest storyReq1 = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/vacation1.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Day 1 in Rome 🇮🇹")
                .build();
        String res1 = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID story1Id = UUID.fromString(objectMapper.readTree(res1).get("data").get("id").asText());

        CreateStoryRequest storyReq2 = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/vacation2.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Day 2 at the Colosseum 🏛️")
                .build();
        String res2 = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID story2Id = UUID.fromString(objectMapper.readTree(res2).get("data").get("id").asText());

        // 2. Creator creates Highlight from both stories
        CreateHighlightRequest createHighlightReq = CreateHighlightRequest.builder()
                .title("Rome 2026")
                .coverImageUrl("https://cloudinary.com/test/rome_cover.jpg")
                .storyIds(List.of(story1Id, story2Id))
                .build();

        String highlightRes = mockMvc.perform(post("/api/v1/highlights")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHighlightReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Rome 2026"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("https://cloudinary.com/test/rome_cover.jpg"))
                .andExpect(jsonPath("$.data.storyCount").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID highlightId = UUID.fromString(objectMapper.readTree(highlightRes).get("data").get("id").asText());

        // 3. Viewer retrieves creator's highlights list
        mockMvc.perform(get("/api/v1/users/{userId}/highlights", creator.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(highlightId.toString()))
                .andExpect(jsonPath("$.data[0].title").value("Rome 2026"))
                .andExpect(jsonPath("$.data[0].storyCount").value(2));

        // 4. Viewer views highlight details (stories playback)
        mockMvc.perform(get("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(highlightId.toString()))
                .andExpect(jsonPath("$.data.title").value("Rome 2026"))
                .andExpect(jsonPath("$.data.author.username").value("story_creator"))
                .andExpect(jsonPath("$.data.stories").isArray())
                .andExpect(jsonPath("$.data.stories.length()").value(2))
                .andExpect(jsonPath("$.data.stories[0].caption").value("Day 1 in Rome 🇮🇹"))
                .andExpect(jsonPath("$.data.stories[1].caption").value("Day 2 at the Colosseum 🏛️"));

        // 5. Unauthorized user (viewer) tries to update creator's highlight -> 403 Forbidden
        UpdateHighlightRequest unauthorizedUpdate = UpdateHighlightRequest.builder()
                .title("Hacked title")
                .build();
        mockMvc.perform(put("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedUpdate)))
                .andExpect(status().isForbidden());

        // 6. Creator updates highlight title
        UpdateHighlightRequest updateReq = UpdateHighlightRequest.builder()
                .title("Best of Rome 🇮🇹")
                .build();
        mockMvc.perform(put("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Best of Rome 🇮🇹"));

        // 7. Creator deletes highlight
        mockMvc.perform(delete("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // 8. Highlight is no longer found -> 404
        mockMvc.perform(get("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Highlight Story Expiry Protection - Expired stories preserved if included in a highlight")
    void cleanupExpiredStories_ProtectsHighlightedStories() throws Exception {
        // 1. Creator posts two stories
        CreateStoryRequest storyReq1 = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/preserved.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Memorable moment to save forever")
                .build();
        String res1 = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID preservedStoryId = UUID.fromString(objectMapper.readTree(res1).get("data").get("id").asText());

        CreateStoryRequest storyReq2 = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/ephemeral.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Just a quick temporary update")
                .build();
        String res2 = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID ephemeralStoryId = UUID.fromString(objectMapper.readTree(res2).get("data").get("id").asText());

        // 2. Creator highlights the first story
        CreateHighlightRequest highlightReq = CreateHighlightRequest.builder()
                .title("Forever Memories")
                .storyIds(List.of(preservedStoryId))
                .build();
        String hlRes = mockMvc.perform(post("/api/v1/highlights")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highlightReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID highlightId = UUID.fromString(objectMapper.readTree(hlRes).get("data").get("id").asText());

        // 3. Simulate passage of 25 hours (both stories expire)
        Story preservedStory = storyRepository.findById(preservedStoryId).orElseThrow();
        preservedStory.setExpiresAt(LocalDateTime.now().minusHours(1));
        storyRepository.save(preservedStory);

        Story ephemeralStory = storyRepository.findById(ephemeralStoryId).orElseThrow();
        ephemeralStory.setExpiresAt(LocalDateTime.now().minusHours(1));
        storyRepository.save(ephemeralStory);

        // 4. Run cleanup
        int cleaned = storyService.cleanupExpiredStories();
        assertThat(cleaned).isGreaterThanOrEqualTo(1);

        // 5. Ephemeral story is gone
        assertThat(storyRepository.findById(ephemeralStoryId)).isEmpty();

        // 6. Highlighted story is preserved
        assertThat(storyRepository.findById(preservedStoryId)).isPresent();

        // 7. Highlight detail still displays the preserved story
        mockMvc.perform(get("/api/v1/highlights/{highlightId}", highlightId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stories[0].id").value(preservedStoryId.toString()))
                .andExpect(jsonPath("$.data.stories[0].caption").value("Memorable moment to save forever"));
    }

    @Test
    @DisplayName("Close Friends Story - Green ring indicator and stealth privacy hiding for non-close-friends")
    void closeFriendsStory_OnlyAccessibleToCloseFriends() throws Exception {
        // 1. Creator adds mentionedUser to close friends
        mockMvc.perform(post("/api/v1/users/close-friends/{friendId}", mentionedUser.getId())
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // 2. Creator checks close friends list
        mockMvc.perform(get("/api/v1/users/close-friends")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("mentioned_friend"));

        // 3. Creator posts a Close-Friends-Only story
        CreateStoryRequest cfStoryReq = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/close_friends_secret.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Secret close friends party! 🤫")
                .closeFriendsOnly(true)
                .build();

        String cfRes = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cfStoryReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.closeFriendsOnly").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID cfStoryId = UUID.fromString(objectMapper.readTree(cfRes).get("data").get("id").asText());

        // 4. Mentioned friend (Close Friend) fetches feed -> story visible with green ring flag
        mockMvc.perform(get("/api/v1/stories/feed")
                        .header("Authorization", "Bearer " + mentionedUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.user.username == 'story_creator')].hasCloseFriendsStories").value(true))
                .andExpect(jsonPath("$.data[?(@.user.username == 'story_creator')].stories[0].closeFriendsOnly").value(true));

        // 5. Viewer (NOT a close friend) fetches feed -> creator's close friends story NOT visible in feed
        mockMvc.perform(get("/api/v1/stories/feed")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.user.username == 'story_creator')]").doesNotExist());

        // 6. Viewer attempts direct view -> 404 NOT FOUND (Stealth Mode)
        mockMvc.perform(post("/api/v1/stories/{storyId}/view", cfStoryId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());

        // 7. Viewer attempts direct like -> 404 NOT FOUND (Stealth Mode)
        mockMvc.perform(post("/api/v1/stories/{storyId}/like", cfStoryId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Story Reply - Viewer replies to story and automatically initiates direct message conversation")
    void storyReply_CreatesDirectMessageInChatThread() throws Exception {
        // 1. Creator posts public story
        CreateStoryRequest storyReq = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/travel_reply.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Exploring the mountains 🏔️")
                .build();

        String res = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID storyId = UUID.fromString(objectMapper.readTree(res).get("data").get("id").asText());

        // 2. Viewer sends reply to story
        StoryReplyRequest replyReq = StoryReplyRequest.builder()
                .content("That view looks breathtaking! Which trek is this?")
                .build();

        mockMvc.perform(post("/api/v1/stories/{storyId}/reply", storyId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("That view looks breathtaking! Which trek is this?"))
                .andExpect(jsonPath("$.data.mediaUrl").value("https://cloudinary.com/test/travel_reply.jpg"));
    }

    @Test
    @DisplayName("Interactive Story Poll - Create poll with options, cast votes, and aggregate percentages")
    void storyPoll_CreateAndVoteLifecycle() throws Exception {
        // 1. Creator posts story with poll
        CreateStoryRequest storyReq = CreateStoryRequest.builder()
                .mediaUrl("https://cloudinary.com/test/poll_story.jpg")
                .mediaType(MediaType.IMAGE)
                .caption("Quick question for everyone!")
                .poll(CreateStoryPollRequest.builder()
                        .question("Beach or Mountains?")
                        .optionA("Beach 🏖️")
                        .optionB("Mountains 🏔️")
                        .build())
                .build();

        String res = mockMvc.perform(post("/api/v1/stories")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.poll.question").value("Beach or Mountains?"))
                .andExpect(jsonPath("$.data.poll.optionA").value("Beach 🏖️"))
                .andExpect(jsonPath("$.data.poll.optionB").value("Mountains 🏔️"))
                .andExpect(jsonPath("$.data.poll.totalVotes").value(0))
                .andReturn().getResponse().getContentAsString();
        UUID storyId = UUID.fromString(objectMapper.readTree(res).get("data").get("id").asText());

        // 2. Viewer votes for Option A (Beach)
        StoryPollVoteRequest voteReq1 = StoryPollVoteRequest.builder()
                .selectedOption(1)
                .build();
        mockMvc.perform(post("/api/v1/stories/{storyId}/poll/vote", storyId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voteCountA").value(1))
                .andExpect(jsonPath("$.data.voteCountB").value(0))
                .andExpect(jsonPath("$.data.totalVotes").value(1))
                .andExpect(jsonPath("$.data.percentA").value(100.0))
                .andExpect(jsonPath("$.data.percentB").value(0.0))
                .andExpect(jsonPath("$.data.userVote").value(1));

        // 3. MentionedUser votes for Option B (Mountains)
        StoryPollVoteRequest voteReq2 = StoryPollVoteRequest.builder()
                .selectedOption(2)
                .build();
        mockMvc.perform(post("/api/v1/stories/{storyId}/poll/vote", storyId)
                        .header("Authorization", "Bearer " + mentionedUserToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voteCountA").value(1))
                .andExpect(jsonPath("$.data.voteCountB").value(1))
                .andExpect(jsonPath("$.data.totalVotes").value(2))
                .andExpect(jsonPath("$.data.percentA").value(50.0))
                .andExpect(jsonPath("$.data.percentB").value(50.0))
                .andExpect(jsonPath("$.data.userVote").value(2));
    }
}
