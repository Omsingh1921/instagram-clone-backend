package com.instagramclone.security;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.follow.entity.FollowRequest;
import com.instagramclone.follow.enums.FollowRequestStatus;
import com.instagramclone.post.dto.request.CreatePostRequest;
import com.instagramclone.post.dto.request.PostMediaRequest;
import com.instagramclone.post.enums.MediaType;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.story.entity.Story;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserBlock;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrivateAccountAndBlockSecurityTest extends BaseIntegrationTest {

    private User owner;
    private User approvedFollower;
    private User stranger;
    private User attacker;

    private String ownerToken;
    private String approvedFollowerToken;
    private String strangerToken;
    private String attackerToken;

    private Post privatePost;
    private Story privateStory;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userRepository.save(User.builder()
                .username("private_owner")
                .email("owner@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        approvedFollower = userRepository.save(User.builder()
                .username("approved_follower")
                .email("follower@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        stranger = userRepository.save(User.builder()
                .username("curious_stranger")
                .email("stranger@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        attacker = userRepository.save(User.builder()
                .username("unauthorized_party")
                .email("attacker@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        // Make owner's account PRIVATE
        userSettingsRepository.save(UserSettings.builder()
                .user(owner)
                .isPrivate(true)
                .build());

        // Approved follower follows owner
        followRepository.save(com.instagramclone.follow.entity.Follow.builder()
                .follower(approvedFollower)
                .following(owner)
                .build());

        ownerToken = jwtTokenProvider.generateTokenForUser(owner.getId(), owner.getUsername(), owner.getEmail());
        approvedFollowerToken = jwtTokenProvider.generateTokenForUser(approvedFollower.getId(), approvedFollower.getUsername(), approvedFollower.getEmail());
        strangerToken = jwtTokenProvider.generateTokenForUser(stranger.getId(), stranger.getUsername(), stranger.getEmail());
        attackerToken = jwtTokenProvider.generateTokenForUser(attacker.getId(), attacker.getUsername(), attacker.getEmail());

        // Create a post for private owner
        privatePost = postRepository.save(Post.builder()
                .user(owner)
                .caption("Private thoughts and exclusive photos #secret")
                .visibility(PostVisibility.PUBLIC)
                .build());

        // Create a story for private owner
        privateStory = storyRepository.save(Story.builder()
                .user(owner)
                .mediaUrl("https://cloudinary.com/private_story.jpg")
                .mediaType(MediaType.IMAGE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());
    }

    @Test
    @DisplayName("Private Account - Owner can view own post by direct ID and listing")
    void privatePost_OwnerCanView() throws Exception {
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(privatePost.getId().toString()));

        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/posts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("Private Account - Approved follower can view post by direct ID and listing")
    void privatePost_ApprovedFollowerCanView() throws Exception {
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + approvedFollowerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(privatePost.getId().toString()));

        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/posts")
                        .header("Authorization", "Bearer " + approvedFollowerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("Private Account - Non-follower cannot view post by direct ID (Forbidden)")
    void privatePost_NonFollowerForbiddenOnDirectId() throws Exception {
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This account is private"));
    }

    @Test
    @DisplayName("Private Account - Non-follower cannot list posts of private account (Forbidden)")
    void privatePost_NonFollowerForbiddenOnUserPostsList() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/posts")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This account is private"));
    }

    @Test
    @DisplayName("Private Account - Story access allowed for owner and approved follower, forbidden for stranger")
    void privateStory_AccessControl() throws Exception {
        // Owner can access
        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/stories")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stories").isNotEmpty());

        // Approved follower can access
        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/stories")
                        .header("Authorization", "Bearer " + approvedFollowerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stories").isNotEmpty());

        // Stranger is forbidden
        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/stories")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is private"));

        // Stranger cannot mark story viewed
        mockMvc.perform(post("/api/v1/stories/" + privateStory.getId() + "/view")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account is private"));
    }

    @Test
    @DisplayName("Follow Request Lifecycle - Create pending request, list, accept, grants content access")
    void followRequest_FullLifecycle_Workflow() throws Exception {
        // Stranger attempts to follow private owner -> creates pending request
        mockMvc.perform(post("/api/v1/users/" + owner.getId() + "/follow")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(false))
                .andExpect(jsonPath("$.data.requested").value(true));

        // Stranger cannot access posts yet
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());

        // Owner views incoming pending follow requests
        mockMvc.perform(get("/api/v1/users/me/follow-requests")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("curious_stranger"));

        // Fetch request ID from database
        FollowRequest pendingReq = followRequestRepository
                .findByRequesterIdAndTargetId(stranger.getId(), owner.getId())
                .orElseThrow();
        assertThat(pendingReq.getStatus()).isEqualTo(FollowRequestStatus.PENDING);

        // Owner accepts the follow request
        mockMvc.perform(post("/api/v1/users/me/follow-requests/" + pendingReq.getId() + "/accept")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(true));

        // Now stranger CAN view private posts and stories
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(privatePost.getId().toString()));

        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/stories")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Follow Request - Rejection removes request and prevents content access")
    void followRequest_Reject_RemovesRequest() throws Exception {
        // Stranger sends request
        mockMvc.perform(post("/api/v1/users/" + owner.getId() + "/follow")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(true));

        FollowRequest req = followRequestRepository
                .findByRequesterIdAndTargetId(stranger.getId(), owner.getId())
                .orElseThrow();

        // Owner rejects
        mockMvc.perform(post("/api/v1/users/me/follow-requests/" + req.getId() + "/reject")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        assertThat(followRequestRepository.findById(req.getId())).isEmpty();
        assertThat(followRepository.existsByFollowerIdAndFollowingId(stranger.getId(), owner.getId())).isFalse();

        // Stranger remains forbidden
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Follow Request - Cancellation removes pending request")
    void followRequest_Cancel_RemovesRequest() throws Exception {
        // Stranger sends request
        mockMvc.perform(post("/api/v1/users/" + owner.getId() + "/follow")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(true));

        // Stranger cancels request
        mockMvc.perform(delete("/api/v1/users/" + owner.getId() + "/follow-request")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow request cancelled"));

        assertThat(followRequestRepository.findByRequesterIdAndTargetId(stranger.getId(), owner.getId())).isEmpty();
    }

    @Test
    @DisplayName("Follow Request - Unauthorized user cannot accept someone else's request")
    void followRequest_UnauthorizedAccept_Forbidden() throws Exception {
        // Stranger sends request to owner
        mockMvc.perform(post("/api/v1/users/" + owner.getId() + "/follow")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk());

        FollowRequest req = followRequestRepository
                .findByRequesterIdAndTargetId(stranger.getId(), owner.getId())
                .orElseThrow();

        // Attacker attempts to accept stranger's request to owner
        mockMvc.perform(post("/api/v1/users/me/follow-requests/" + req.getId() + "/accept")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Remove Follower - Owner can remove follower and revoke private content access")
    void removeFollower_RevokesContentAccess() throws Exception {
        // Approved follower can view initially
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + approvedFollowerToken))
                .andExpect(status().isOk());

        // Owner removes approved follower
        mockMvc.perform(delete("/api/v1/users/me/followers/" + approvedFollower.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follower removed successfully"));

        // Now follower is no longer following
        assertThat(followRepository.existsByFollowerIdAndFollowingId(approvedFollower.getId(), owner.getId())).isFalse();

        // Access to private posts is now forbidden
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + approvedFollowerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Block Privacy - Blocked user cannot view blocker profile, posts, or stories (404 Not Found)")
    void blockPrivacy_BlockedUserDirectAccess_ReturnsNotFound() throws Exception {
        // Owner blocks stranger
        userBlockRepository.save(UserBlock.builder()
                .blocker(owner)
                .blocked(stranger)
                .build());

        // Stranger tries to get owner's profile -> 404
        mockMvc.perform(get("/api/v1/users/" + owner.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        // Stranger tries to get owner's post -> 404
        mockMvc.perform(get("/api/v1/posts/" + privatePost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        // Stranger tries to get owner's stories -> 404
        mockMvc.perform(get("/api/v1/users/" + owner.getId() + "/stories")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Block Privacy - Blocked user's public content is excluded from search")
    void blockPrivacy_Search_ExcludesBlockedUsers() throws Exception {
        User publicUser = userRepository.save(User.builder()
                .username("public_superstar")
                .email("superstar@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        // Public user blocks stranger
        userBlockRepository.save(UserBlock.builder()
                .blocker(publicUser)
                .blocked(stranger)
                .build());

        // Stranger searches for publicUser -> excluded
        mockMvc.perform(get("/api/v1/search/users?query=superstar")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("Public Account - Behavior is completely preserved for public accounts")
    void publicAccount_NormalAccessPreserved() throws Exception {
        User normalUser = userRepository.save(User.builder()
                .username("normal_public")
                .email("public@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        Post publicPost = postRepository.save(Post.builder()
                .user(normalUser)
                .caption("Public sunshine for everyone")
                .visibility(PostVisibility.PUBLIC)
                .build());

        // Stranger can view public post without following
        mockMvc.perform(get("/api/v1/posts/" + publicPost.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publicPost.getId().toString()));

        // Stranger follows public user immediately
        mockMvc.perform(post("/api/v1/users/" + normalUser.getId() + "/follow")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(true))
                .andExpect(jsonPath("$.data.requested").value(false));
    }
}
