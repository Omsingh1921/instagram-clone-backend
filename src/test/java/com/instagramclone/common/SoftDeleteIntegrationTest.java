package com.instagramclone.common;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.engagement.entity.Comment;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SoftDeleteIntegrationTest extends BaseIntegrationTest {

    private User testUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        testUser = User.builder()
                .username("soft_delete_user")
                .email("softdelete@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        Profile profile = Profile.builder()
                .user(testUser)
                .displayName("Soft Delete User")
                .build();
        testUser.setProfile(profile);
        testUser = userRepository.save(testUser);

        userToken = jwtTokenProvider.generateTokenForUser(testUser.getId(), testUser.getUsername(), testUser.getEmail());
    }

    @Test
    @DisplayName("User Soft Delete - Calling DELETE /api/v1/users/me marks user as soft-deleted and excludes from active queries")
    void userSoftDelete_ShouldMarkFlagsAndExcludeFromQueries() throws Exception {
        // Initially active and not deleted
        assertThat(testUser.getIsDeleted()).isFalse();
        assertThat(testUser.getIsActive()).isTrue();
        assertThat(userRepository.findByIdAndIsDeletedFalse(testUser.getId())).isPresent();

        // Perform soft delete via API
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(com.instagramclone.user.constants.UserConstants.MSG_ACCOUNT_DELETED_SUCCESS));

        // Row still exists in DB (not hard deleted)
        Optional<User> rawDbUser = userRepository.findById(testUser.getId());
        assertThat(rawDbUser).isPresent();
        User deletedUser = rawDbUser.get();
        assertThat(deletedUser.getIsDeleted()).isTrue();
        assertThat(deletedUser.getIsActive()).isFalse();
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deletedUser.getDeletedAt()).isNotNull();

        // Excluded from findByIdAndIsDeletedFalse
        assertThat(userRepository.findByIdAndIsDeletedFalse(testUser.getId())).isEmpty();

        // Excluded from searchUsers
        assertThat(userRepository.searchUsers("soft_delete_user", org.springframework.data.domain.Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Account Reactivation - Logging in within 15 days reactivates soft-deleted account")
    void userLoginWithin15Days_ShouldReactivateAccount() throws Exception {
        // Soft delete user
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deletedUser.getIsDeleted()).isTrue();
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);

        // Simulate login 2 days later (well within 15 days)
        com.instagramclone.auth.dto.request.LoginRequest loginRequest = new com.instagramclone.auth.dto.request.LoginRequest(
                "soft_delete_user", "Password123!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());

        // Verify account is automatically reactivated
        User reactivatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(reactivatedUser.getIsDeleted()).isFalse();
        assertThat(reactivatedUser.getIsActive()).isTrue();
        assertThat(reactivatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reactivatedUser.getDeletedAt()).isNull();
        assertThat(userRepository.findByIdAndIsDeletedFalse(testUser.getId())).isPresent();
    }

    @Test
    @DisplayName("Permanent Deletion - Logging in after 15 days recovery window is permanently rejected")
    void userLoginAfter15Days_ShouldRejectLogin() throws Exception {
        // Set user to soft deleted 16 days ago
        testUser.softDelete();
        testUser.setDeletedAt(java.time.LocalDateTime.now().minusDays(16));
        userRepository.save(testUser);

        com.instagramclone.auth.dto.request.LoginRequest loginRequest = new com.instagramclone.auth.dto.request.LoginRequest(
                "soft_delete_user", "Password123!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        // Account must remain deleted
        User userAfterAttempt = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userAfterAttempt.getIsDeleted()).isTrue();
        assertThat(userAfterAttempt.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("Post Soft Delete - Calling DELETE /api/v1/posts/{id} soft-deletes post preserving DB record")
    void postSoftDelete_ShouldPreserveRecordAndExcludeFromTimeline() throws Exception {
        Post post = Post.builder()
                .user(testUser)
                .caption("This is a post to be soft deleted")
                .visibility(PostVisibility.PUBLIC)
                .build();
        post = postRepository.save(post);
        UUID postId = post.getId();

        assertThat(post.getIsDeleted()).isFalse();
        assertThat(post.getIsActive()).isTrue();

        // Delete post
        mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Row still exists in database
        Optional<Post> rawPost = postRepository.findById(postId);
        assertThat(rawPost).isPresent();
        assertThat(rawPost.get().getIsDeleted()).isTrue();
        assertThat(rawPost.get().getIsActive()).isFalse();

        // findByIdAndIsDeletedFalse does not return it
        assertThat(postRepository.findByIdAndIsDeletedFalse(postId)).isEmpty();

        // Timeline does not return it
        assertThat(postRepository.findTimelinePosts(testUser.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Comment Soft Delete - Calling DELETE /api/v1/comments/{id} marks comment isDeleted=true")
    void commentSoftDelete_ShouldMarkDeletedAndExcludeFromCount() throws Exception {
        Post post = Post.builder()
                .user(testUser)
                .caption("Post for comment test")
                .visibility(PostVisibility.PUBLIC)
                .build();
        post = postRepository.save(post);

        Comment comment = Comment.builder()
                .post(post)
                .user(testUser)
                .text("Test comment")
                .build();
        comment = commentRepository.save(comment);
        UUID commentId = comment.getId();

        assertThat(commentRepository.countByPostIdAndIsDeletedFalse(post.getId())).isEqualTo(1L);

        // Delete comment
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Row still exists in DB
        Optional<Comment> rawComment = commentRepository.findById(commentId);
        assertThat(rawComment).isPresent();
        assertThat(rawComment.get().getIsDeleted()).isTrue();
        assertThat(rawComment.get().getIsActive()).isFalse();

        // Excluded from active count and root comments
        assertThat(commentRepository.countByPostIdAndIsDeletedFalse(post.getId())).isEqualTo(0L);
        assertThat(commentRepository.findByPostIdAndParentCommentIsNullAndIsDeletedFalse(post.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Access Control - When account is soft deleted, only ADMIN can fetch data, non-admins get 404")
    void deletedAccount_OnlyAdminCanFetchData() throws Exception {
        // Create viewer user
        User viewer = User.builder()
                .username("regular_viewer")
                .email("viewer@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        viewer = userRepository.save(viewer);
        String viewerToken = jwtTokenProvider.generateTokenForUser(viewer.getId(), viewer.getUsername(), viewer.getEmail());

        // Create admin user
        User admin = User.builder()
                .username("admin_user")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_ADMIN)
                .emailVerified(true)
                .build();
        admin = userRepository.save(admin);
        String adminToken = jwtTokenProvider.generateTokenForUser(admin.getId(), admin.getUsername(), admin.getEmail());

        // Create a post for testUser
        Post post = Post.builder()
                .user(testUser)
                .caption("Author will be deleted")
                .visibility(PostVisibility.PUBLIC)
                .build();
        postRepository.save(post);

        // Soft delete testUser
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 1. Regular viewer tries to fetch deleted user's profile -> 404 Not Found
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());

        // 2. Regular viewer tries to fetch deleted user's posts -> 404 Not Found
        mockMvc.perform(get("/api/v1/users/{userId}/posts", testUser.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());

        // 3. ADMIN fetches deleted user's profile -> 200 OK!
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(testUser.getId().toString()));

        // 4. ADMIN fetches deleted user's posts -> 200 OK!
        mockMvc.perform(get("/api/v1/users/{userId}/posts", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Deactivation - Deactivated account data cannot be fetched except by admin, reactivates on login")
    void deactivatedAccount_OnlyAdminCanFetchData_AndReactivatesOnLogin() throws Exception {
        // Create viewer user
        User viewer = User.builder()
                .username("viewer_two")
                .email("viewer2@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        viewer = userRepository.save(viewer);
        String viewerToken = jwtTokenProvider.generateTokenForUser(viewer.getId(), viewer.getUsername(), viewer.getEmail());

        // Create admin user
        User admin = User.builder()
                .username("admin_two")
                .email("admin2@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_ADMIN)
                .emailVerified(true)
                .build();
        admin = userRepository.save(admin);
        String adminToken = jwtTokenProvider.generateTokenForUser(admin.getId(), admin.getUsername(), admin.getEmail());

        // User deactivates account
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        User deactivated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deactivated.getIsActive()).isFalse();
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);

        // 1. Viewer tries to fetch deactivated user profile -> 404 Not Found
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());

        // 2. ADMIN fetches deactivated user profile -> 200 OK
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(testUser.getId().toString()));

        // 3. User logs back in -> Account reactivated
        com.instagramclone.auth.dto.request.LoginRequest loginReq = new com.instagramclone.auth.dto.request.LoginRequest(
                "soft_delete_user", "Password123!");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());

        User reactivated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(reactivated.getIsActive()).isTrue();
        assertThat(reactivated.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // 4. Viewer can now fetch profile again -> 200 OK
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Followers & Following Count & List - Deleted and deactivated accounts are not counted and do not appear in lists")
    void deletedOrDeactivatedFollower_ShouldNotBeCountedOrReturnedInFollowList() throws Exception {
        // Create userB who follows testUser, and testUser follows userB
        User userB = User.builder()
                .username("follower_bob")
                .email("bob@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userB = userRepository.save(userB);
        String bobToken = jwtTokenProvider.generateTokenForUser(userB.getId(), userB.getUsername(), userB.getEmail());

        // userB follows testUser
        com.instagramclone.follow.entity.Follow follow1 = com.instagramclone.follow.entity.Follow.builder()
                .follower(userB)
                .following(testUser)
                .build();
        followRepository.save(follow1);

        // testUser follows userB
        com.instagramclone.follow.entity.Follow follow2 = com.instagramclone.follow.entity.Follow.builder()
                .follower(testUser)
                .following(userB)
                .build();
        followRepository.save(follow2);

        // Initially: testUser has 1 follower and 1 following
        assertThat(followRepository.countByFollowingId(testUser.getId())).isEqualTo(1L);
        assertThat(followRepository.countByFollowerId(testUser.getId())).isEqualTo(1L);

        // API verify: fetching followers & following list
        mockMvc.perform(get("/api/v1/users/{userId}/followers", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        mockMvc.perform(get("/api/v1/users/{userId}/following", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // Now userB deactivates their account
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        // follower count of testUser must now be 0 (userB is deactivated)
        assertThat(followRepository.countByFollowingId(testUser.getId())).isEqualTo(0L);
        // following count of testUser must also be 0 (userB is deactivated)
        assertThat(followRepository.countByFollowerId(testUser.getId())).isEqualTo(0L);

        // Fetching followers list of testUser returns 0 items for regular user
        mockMvc.perform(get("/api/v1/users/{userId}/followers", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Fetching following list of testUser returns 0 items for regular user
        mockMvc.perform(get("/api/v1/users/{userId}/following", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Profile endpoint reflects 0 follower and 0 following counts
        mockMvc.perform(get("/api/v1/users/{userId}", testUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followerCount").value(0))
                .andExpect(jsonPath("$.data.followingCount").value(0));
    }

    @Test
    @DisplayName("Suspended Account - Login must be rejected with 403 and message saying account is suspended")
    void suspendedAccount_LoginShouldBeRejectedWithMessage() throws Exception {
        // Create suspended user
        User suspendedUser = User.builder()
                .username("suspended_charlie")
                .email("charlie@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.SUSPENDED)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userRepository.save(suspendedUser);

        // Attempt login with username
        com.instagramclone.auth.dto.request.LoginRequest loginReq = new com.instagramclone.auth.dto.request.LoginRequest(
                "suspended_charlie", "Password123!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_SUSPENDED))
                .andExpect(jsonPath("$.errors[0]").value("ACCOUNT_SUSPENDED"));

        // Attempt login with email
        com.instagramclone.auth.dto.request.LoginRequest emailLoginReq = new com.instagramclone.auth.dto.request.LoginRequest(
                "charlie@example.com", "Password123!");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailLoginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(com.instagramclone.user.constants.UserConstants.ERR_ACCOUNT_SUSPENDED))
                .andExpect(jsonPath("$.errors[0]").value("ACCOUNT_SUSPENDED"));
    }

    @Test
    @DisplayName("Lifecycle - Soft-deleted account has sessions revoked and cannot access protected resources with old token")
    void softDeletedAccount_TokensRevoked_AndCannotAccessProtectedResources() throws Exception {
        // testUser is logged in and has userToken
        // 1. Verify user can access /api/v1/users/me currently
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 2. Soft delete testUser
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 3. Verify in DB that all refresh tokens for testUser are now revoked
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(testUser.getId());
        assertThat(activeTokens).isEmpty();

        // 4. Verify that trying to access protected endpoint with the old JWT access token is REJECTED
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());

        // 5. User logs in with valid credentials within grace period -> Reactivates and gets new token
        com.instagramclone.auth.dto.request.LoginRequest loginReq = new com.instagramclone.auth.dto.request.LoginRequest(
                "soft_delete_user", "Password123!");

        org.springframework.test.web.servlet.MvcResult loginResult = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String newToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 6. New token can now access protected resources!
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("soft_delete_user"));
    }

    @Test
    @DisplayName("Lifecycle - Deactivated account has sessions revoked and cannot access protected resources with old token")
    void deactivatedAccount_TokensRevoked_AndCannotAccessProtectedResources() throws Exception {
        // 1. Verify user can access /api/v1/users/me currently
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 2. Deactivate testUser
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 3. Verify in DB that all refresh tokens for testUser are now revoked
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(testUser.getId());
        assertThat(activeTokens).isEmpty();

        // 4. Verify that trying to access protected endpoint with the old JWT access token is REJECTED
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());

        // 5. User logs in with valid credentials -> Reactivates and gets new token
        com.instagramclone.auth.dto.request.LoginRequest loginReq = new com.instagramclone.auth.dto.request.LoginRequest(
                "soft_delete_user", "Password123!");

        org.springframework.test.web.servlet.MvcResult loginResult = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String newToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 6. New token can now access protected resources!
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("soft_delete_user"));
    }

    @Test
    @DisplayName("Blocking - Blocking a user deletes reciprocal pending follow requests")
    void blockingUser_ShouldDeletePendingFollowRequests() throws Exception {
        // Create user target
        User target = User.builder()
                .username("private_target")
                .email("target@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        target = userRepository.save(target);

        // Make target private
        userSettingsRepository.save(com.instagramclone.user.entity.UserSettings.builder()
                .user(target)
                .isPrivate(true)
                .build());

        // testUser sends follow request to target
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/{userId}/follow", target.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requested").value(true));

        assertThat(followRequestRepository.findByRequesterIdAndTargetId(testUser.getId(), target.getId())).isPresent();

        // Now testUser blocks target
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/users/{userId}/block", target.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Verify pending follow request has been deleted
        assertThat(followRequestRepository.findByRequesterIdAndTargetId(testUser.getId(), target.getId())).isEmpty();
    }
}
