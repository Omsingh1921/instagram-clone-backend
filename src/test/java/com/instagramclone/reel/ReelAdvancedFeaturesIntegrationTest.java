package com.instagramclone.reel;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.follow.entity.Follow;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.enums.ReelStatus;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReelAdvancedFeaturesIntegrationTest extends BaseIntegrationTest {

    private User publicCreator;
    private User privateCreator;
    private User viewer;
    private User followerViewer;
    private User blockedViewer;

    private String publicCreatorToken;
    private String privateCreatorToken;
    private String viewerToken;
    private String followerViewerToken;
    private String blockedViewerToken;

    private Reel publicReel;
    private Reel privateReel;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        publicCreator = userRepository.save(User.builder()
                .username("public_creator")
                .email("public_creator@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        privateCreator = userRepository.save(User.builder()
                .username("private_creator")
                .email("private_creator@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        viewer = userRepository.save(User.builder()
                .username("reel_viewer_one")
                .email("viewer_one@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        followerViewer = userRepository.save(User.builder()
                .username("reel_follower_viewer")
                .email("follower_viewer@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        blockedViewer = userRepository.save(User.builder()
                .username("reel_blocked_viewer")
                .email("blocked_viewer@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(publicCreator)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(privateCreator)
                .isPrivate(true)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(viewer)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(followerViewer)
                .isPrivate(false)
                .build());

        userSettingsRepository.save(UserSettings.builder()
                .user(blockedViewer)
                .isPrivate(false)
                .build());

        // followerViewer follows privateCreator
        followRepository.save(Follow.builder()
                .follower(followerViewer)
                .following(privateCreator)
                .build());

        // publicCreator blocks blockedViewer
        userBlockRepository.save(UserBlock.builder()
                .blocker(publicCreator)
                .blocked(blockedViewer)
                .build());

        publicCreatorToken = jwtTokenProvider.generateTokenForUser(publicCreator.getId(), publicCreator.getUsername(), publicCreator.getEmail());
        privateCreatorToken = jwtTokenProvider.generateTokenForUser(privateCreator.getId(), privateCreator.getUsername(), privateCreator.getEmail());
        viewerToken = jwtTokenProvider.generateTokenForUser(viewer.getId(), viewer.getUsername(), viewer.getEmail());
        followerViewerToken = jwtTokenProvider.generateTokenForUser(followerViewer.getId(), followerViewer.getUsername(), followerViewer.getEmail());
        blockedViewerToken = jwtTokenProvider.generateTokenForUser(blockedViewer.getId(), blockedViewer.getUsername(), blockedViewer.getEmail());

        publicReel = reelRepository.save(Reel.builder()
                .user(publicCreator)
                .videoUrl("https://cloudinary.com/test/public_reel.mp4")
                .caption("Public viral reel")
                .durationMs(12000L)
                .status(ReelStatus.READY)
                .build());

        privateReel = reelRepository.save(Reel.builder()
                .user(privateCreator)
                .videoUrl("https://cloudinary.com/test/private_reel.mp4")
                .caption("Private personal reel")
                .durationMs(15000L)
                .status(ReelStatus.READY)
                .build());
    }

    @Test
    @DisplayName("Who Liked This Reel - Return users who liked reel with follow state")
    void getReelLikes_ShouldReturnFollowUserResponses() throws Exception {
        // Viewer and followerViewer like publicReel
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", publicReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reels/{reelId}/like", publicReel.getId())
                        .header("Authorization", "Bearer " + followerViewerToken))
                .andExpect(status().isOk());

        // publicCreator also follows followerViewer to test followedByCurrentUser flag
        followRepository.save(Follow.builder()
                .follower(publicCreator)
                .following(followerViewer)
                .build());

        // publicCreator fetches who liked their reel
        mockMvc.perform(get("/api/v1/reels/{reelId}/likes", publicReel.getId())
                        .header("Authorization", "Bearer " + publicCreatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[?(@.username == 'reel_follower_viewer')].followedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.content[?(@.username == 'reel_viewer_one')].followedByCurrentUser").value(false));
    }

    @Test
    @DisplayName("Who Liked This Reel - Enforces privacy and block access rules")
    void getReelLikes_ShouldEnforcePrivacyAndBlockChecks() throws Exception {
        // followerViewer likes privateReel
        mockMvc.perform(post("/api/v1/reels/{reelId}/like", privateReel.getId())
                        .header("Authorization", "Bearer " + followerViewerToken))
                .andExpect(status().isOk());

        // Stranger viewer (non-follower) attempts to get likes on private reel -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reels/{reelId}/likes", privateReel.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Blocked viewer attempts to get likes on public reel -> 404 Not Found
        mockMvc.perform(get("/api/v1/reels/{reelId}/likes", publicReel.getId())
                        .header("Authorization", "Bearer " + blockedViewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Global Reels Feed - Excludes private accounts for strangers and excludes blocked users")
    void getReelsFeed_ShouldFilterPrivateAndBlockedAccounts() throws Exception {
        // 1. Stranger viewer fetches global feed:
        // Must see publicReel, but MUST NOT see privateReel!
        mockMvc.perform(get("/api/v1/reels")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.caption == 'Public viral reel')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.caption == 'Private personal reel')]").doesNotExist());

        // 2. followerViewer (who follows privateCreator) fetches global feed:
        // Should see BOTH publicReel and privateReel!
        mockMvc.perform(get("/api/v1/reels")
                        .header("Authorization", "Bearer " + followerViewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.caption == 'Public viral reel')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.caption == 'Private personal reel')]").exists());

        // 3. blockedViewer (blocked by publicCreator) fetches global feed:
        // Must NOT see publicReel!
        mockMvc.perform(get("/api/v1/reels")
                        .header("Authorization", "Bearer " + blockedViewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.caption == 'Public viral reel')]").doesNotExist());
    }

    @Test
    @DisplayName("Reel Creation - Duration Bounds Validation (3s - 90s)")
    void createReel_ShouldValidateDurationBounds() throws Exception {
        // Less than 3s (e.g. 2,000ms) -> 400 Bad Request
        CreateReelRequest tooShort = CreateReelRequest.builder()
                .videoUrl("https://cloudinary.com/test/too_short.mp4")
                .caption("Too short")
                .durationMs(2000L)
                .build();

        mockMvc.perform(post("/api/v1/reels")
                        .header("Authorization", "Bearer " + publicCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShort)))
                .andExpect(status().isBadRequest());

        // More than 90s (e.g. 95,000ms) -> 400 Bad Request
        CreateReelRequest tooLong = CreateReelRequest.builder()
                .videoUrl("https://cloudinary.com/test/too_long.mp4")
                .caption("Too long")
                .durationMs(95000L)
                .build();

        mockMvc.perform(post("/api/v1/reels")
                        .header("Authorization", "Bearer " + publicCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLong)))
                .andExpect(status().isBadRequest());

        // Valid duration (15,000ms) -> 201 Created
        CreateReelRequest validReel = CreateReelRequest.builder()
                .videoUrl("https://cloudinary.com/test/valid.mp4")
                .caption("Valid reel")
                .durationMs(15000L)
                .build();

        mockMvc.perform(post("/api/v1/reels")
                        .header("Authorization", "Bearer " + publicCreatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReel)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.durationMs").value(15000));
    }
}
