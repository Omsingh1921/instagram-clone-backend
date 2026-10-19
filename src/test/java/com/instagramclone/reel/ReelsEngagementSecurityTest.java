package com.instagramclone.reel;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.follow.entity.Follow;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.reel.dto.request.CreateReelCommentRequest;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReelsEngagementSecurityTest extends BaseIntegrationTest {

    private User creator;
    private User viewer;
    private User secondViewer;
    private User blockedUser;

    private String creatorToken;
    private String viewerToken;
    private String secondViewerToken;
    private String blockedUserToken;

    private Reel testReel;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        creator = userRepository.save(User.builder()
                .username("reel_creator")
                .email("creator@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        viewer = userRepository.save(User.builder()
                .username("reel_viewer")
                .email("viewer@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        secondViewer = userRepository.save(User.builder()
                .username("second_viewer")
                .email("second_viewer@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        blockedUser = userRepository.save(User.builder()
                .username("blocked_user")
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
                .user(viewer)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(secondViewer)
                .isPrivate(false)
                .build());

        creatorToken = jwtTokenProvider.generateTokenForUser(creator.getId(), creator.getUsername(), creator.getEmail());
        viewerToken = jwtTokenProvider.generateTokenForUser(viewer.getId(), viewer.getUsername(), viewer.getEmail());
        secondViewerToken = jwtTokenProvider.generateTokenForUser(secondViewer.getId(), secondViewer.getUsername(), secondViewer.getEmail());
        blockedUserToken = jwtTokenProvider.generateTokenForUser(blockedUser.getId(), blockedUser.getUsername(), blockedUser.getEmail());

        testReel = reelRepository.save(Reel.builder()
                .user(creator)
                .videoUrl("https://cloudinary.com/test/video.mp4")
                .caption("Sunset vibes")
                .durationMs(15000L)
                .build());
    }

    @Test
    @DisplayName("Like & Unlike - Toggling reel like persists state, updates count, and generates single notification")
    void likeAndUnlike_ShouldToggleStateAndMaintainNotificationIntegrity() throws Exception {
        // 1. Initial Like by viewer
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(reelLikeRepository.countByReelId(testReel.getId())).isEqualTo(1L);
        assertThat(reelLikeRepository.existsByReelIdAndUserId(testReel.getId(), viewer.getId())).isTrue();

        // Verify single notification created for creator
        long notificationCount = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(creator.getId()) &&
                             n.getType() == NotificationType.REEL_LIKED)
                .count();
        assertThat(notificationCount).isEqualTo(1L);

        // 2. Unlike by viewer
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        assertThat(reelLikeRepository.countByReelId(testReel.getId())).isEqualTo(0L);
        assertThat(reelLikeRepository.existsByReelIdAndUserId(testReel.getId(), viewer.getId())).isFalse();

        // Unlike must NOT generate new notifications
        long notificationCountAfterUnlike = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(creator.getId()) &&
                             n.getType() == NotificationType.REEL_LIKED)
                .count();
        assertThat(notificationCountAfterUnlike).isEqualTo(1L);
    }

    @Test
    @DisplayName("Concurrency - Concurrent likes and unlikes prevent duplicates and maintain consistent counts")
    void concurrentLikesAndUnlikes_ShouldMaintainConsistency() throws Exception {
        int threadCount = 6;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User u = userRepository.save(User.builder()
                    .username("concurrent_user_" + i)
                    .email("concurrent_" + i + "@test.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .build());
            tokens.add(jwtTokenProvider.generateTokenForUser(u.getId(), u.getUsername(), u.getEmail()));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (String userToken : tokens) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                            .header("Authorization", "Bearer " + userToken));
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        long likeCount = reelLikeRepository.countByReelId(testReel.getId());
        assertThat(likeCount).isEqualTo((long) threadCount);

        // Now concurrent unlike
        CountDownLatch unlikeStartLatch = new CountDownLatch(1);
        CountDownLatch unlikeFinishLatch = new CountDownLatch(threadCount);

        for (String userToken : tokens) {
            executor.submit(() -> {
                try {
                    unlikeStartLatch.await();
                    mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                            .header("Authorization", "Bearer " + userToken));
                } catch (Exception ignored) {
                } finally {
                    unlikeFinishLatch.countDown();
                }
            });
        }

        unlikeStartLatch.countDown();
        boolean unlikesCompleted = unlikeFinishLatch.await(10, TimeUnit.SECONDS);
        assertThat(unlikesCompleted).isTrue();

        long afterUnlikeCount = reelLikeRepository.countByReelId(testReel.getId());
        assertThat(afterUnlikeCount).isEqualTo(0L);

        executor.shutdown();
    }


    @Test
    @DisplayName("Comments - Create, list paginated, and delete with ownership enforcement")
    void comments_ShouldSupportLifecycleAndPermissionRules() throws Exception {
        CreateReelCommentRequest commentRequest = CreateReelCommentRequest.builder()
                .text("Amazing video!")
                .build();

        // 1. Viewer comments on reel
        String commentResponse = mockMvc.perform(post("/api/v1/reels/{reelId}/comments", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.text").value("Amazing video!"))
                .andExpect(jsonPath("$.data.author.username").value("reel_viewer"))
                .andReturn().getResponse().getContentAsString();

        UUID commentId = UUID.fromString(objectMapper.readTree(commentResponse).get("data").get("id").asText());
        assertThat(reelCommentRepository.countByReelId(testReel.getId())).isEqualTo(1L);

        // Verify comment notification created for creator
        boolean hasCommentNotification = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getRecipient().getId().equals(creator.getId()) &&
                               n.getType() == NotificationType.REEL_COMMENTED);
        assertThat(hasCommentNotification).isTrue();

        // 2. List paginated comments
        mockMvc.perform(get("/api/v1/reels/{reelId}/comments", testReel.getId())
                        .header("Authorization", "Bearer " + secondViewerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.data.content[0].text").value("Amazing video!"));

        // 3. Second viewer (stranger) attempts to delete viewer's comment -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/reels/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + secondViewerToken))
                .andExpect(status().isForbidden());

        // 4. Reel creator (owner of reel) CAN delete any comment on their reel -> 200 OK
        mockMvc.perform(delete("/api/v1/reels/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        assertThat(reelCommentRepository.existsById(commentId)).isFalse();
    }

    @Test
    @DisplayName("Views - Deduplicated playback tracking and count increment")
    void views_ShouldDeduplicatePerViewerAndTrackCounts() throws Exception {
        // 1. Viewer records view
        mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewed").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(1L);

        // 2. Same viewer records view again -> Deduplicated, count remains 1
        mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewed").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(1L);

        // 3. Second viewer records view -> Count increments to 2
        mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                        .header("Authorization", "Bearer " + secondViewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewed").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(2));

        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(2L);
    }

    @Test
    @DisplayName("Privacy Enforcement - Private creator restricts reel interactions for non-followers")
    void privateAccount_ShouldEnforceGatingAcrossReelEndpoints() throws Exception {
        // Set creator account to private
        UserSettings settings = userSettingsRepository.findByUserId(creator.getId()).orElseThrow();
        settings.setPrivate(true);
        userSettingsRepository.save(settings);

        // Non-follower viewer attempts getReel -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reels/{reelId}", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Non-follower viewer attempts like -> 403 Forbidden
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Non-follower viewer attempts comment -> 403 Forbidden
        CreateReelCommentRequest commentReq = CreateReelCommentRequest.builder()
                .text("Cannot comment")
                .build();
        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isForbidden());

        // Non-follower viewer attempts view -> 403 Forbidden
        mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Now viewer follows creator
        followRepository.save(Follow.builder()
                .follower(viewer)
                .following(creator)
                .build());

        // Approved follower CAN view and interact
        mockMvc.perform(get("/api/v1/reels/{reelId}", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.caption").value("Sunset vibes"));

        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));
    }

    @Test
    @DisplayName("Block Enforcement - Bidirectional blocks return 404 to prevent information disclosure")
    void blockEnforcement_ShouldReturn404ForBlockedUsers() throws Exception {
        // Creator blocks blockedUser
        userBlockRepository.save(UserBlock.builder()
                .blocker(creator)
                .blocked(blockedUser)
                .build());

        // Blocked user attempts getReel -> 404 Not Found
        mockMvc.perform(get("/api/v1/reels/{reelId}", testReel.getId())
                        .header("Authorization", "Bearer " + blockedUserToken))
                .andExpect(status().isNotFound());

        // Blocked user attempts like -> 404 Not Found
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + blockedUserToken))
                .andExpect(status().isNotFound());

        // Blocked user attempts comment -> 404 Not Found
        CreateReelCommentRequest req = CreateReelCommentRequest.builder().text("Blocked text").build();
        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", testReel.getId())
                        .header("Authorization", "Bearer " + blockedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cascade Deletion - Deleting reel cleans up all likes, comments, and views")
    void deleteReel_ShouldCascadeCleanEngagementRecords() throws Exception {
        // Add like, comment, and view
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        CreateReelCommentRequest req = CreateReelCommentRequest.builder().text("Nice reel").build();
        mockMvc.perform(post("/api/v1/reels/{reelId}/comments", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        assertThat(reelLikeRepository.countByReelId(testReel.getId())).isEqualTo(1L);
        assertThat(reelCommentRepository.countByReelId(testReel.getId())).isEqualTo(1L);
        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(1L);

        // Creator deletes reel
        mockMvc.perform(delete("/api/v1/reels/{reelId}", testReel.getId())
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        assertThat(reelRepository.existsById(testReel.getId())).isFalse();
        assertThat(reelLikeRepository.countByReelId(testReel.getId())).isEqualTo(0L);
        assertThat(reelCommentRepository.countByReelId(testReel.getId())).isEqualTo(0L);
        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(0L);
    }

    @Test
    @DisplayName("Concurrency Audit - Competing toggle requests from same user complete with 200 OK and zero rollback errors")
    void competingToggleRequests_SameUser_ShouldRemainConsistentAndNeverRollbackOnly() throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int status = mockMvc.perform(post("/api/v1/reels/{reelId}/like", testReel.getId())
                                    .header("Authorization", "Bearer " + viewerToken))
                            .andReturn().getResponse().getStatus();
                    statusCodes.add(status);
                } catch (Exception ex) {
                    statusCodes.add(500);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // All concurrent requests MUST succeed with 200 OK (no 500 UnexpectedRollbackException)
        assertThat(statusCodes).hasSize(threads);
        assertThat(statusCodes).allMatch(status -> status == 200);

        long finalCount = reelLikeRepository.countByReelId(testReel.getId());
        assertThat(finalCount).isIn(0L, 1L);

        executor.shutdown();
    }

    @Test
    @DisplayName("Concurrency Audit - Simultaneous duplicate views from same user produce exactly one durable view")
    void simultaneousDuplicateViews_SameUser_ShouldProduceSingleDurableView() throws Exception {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int status = mockMvc.perform(post("/api/v1/reels/{reelId}/view", testReel.getId())
                                    .header("Authorization", "Bearer " + viewerToken))
                            .andReturn().getResponse().getStatus();
                    statusCodes.add(status);
                } catch (Exception ex) {
                    statusCodes.add(500);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // All requests must succeed with 200 OK
        assertThat(statusCodes).hasSize(threads);
        assertThat(statusCodes).allMatch(status -> status == 200);

        // Exactly one durable view in MySQL
        assertThat(reelViewRepository.countByReelId(testReel.getId())).isEqualTo(1L);

        executor.shutdown();
    }
}

