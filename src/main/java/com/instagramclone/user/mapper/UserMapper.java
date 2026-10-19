package com.instagramclone.user.mapper;

import com.instagramclone.user.common.UserSummary;
import com.instagramclone.user.dto.response.UserProfileResponse;
import com.instagramclone.user.dto.response.UserResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.Profile;
import com.instagramclone.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        Profile profile = user.getProfile();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .websiteUrl(profile != null ? profile.getWebsiteUrl() : null)
                .status(user.getStatus())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserProfileResponse toUserProfileResponse(User user, Boolean followedByCurrentUser) {
        return toUserProfileResponse(user, 0L, 0L, 0L, followedByCurrentUser);
    }

    public UserProfileResponse toUserProfileResponse(User user, long postCount, long followerCount, long followingCount, Boolean followedByCurrentUser) {
        return toUserProfileResponse(user, postCount, followerCount, followingCount, followedByCurrentUser, false, null);
    }

    public UserProfileResponse toUserProfileResponse(User user, long postCount, long followerCount, long followingCount, Boolean followedByCurrentUser, boolean isPrivate, Boolean followRequested) {
        if (user == null) {
            return null;
        }

        Profile profile = user.getProfile();
        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .websiteUrl(profile != null ? profile.getWebsiteUrl() : null)
                .postCount(postCount)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .followedByCurrentUser(followedByCurrentUser)
                .isPrivate(isPrivate)
                .followRequestedByCurrentUser(followRequested)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserSummaryResponse toUserSummaryResponse(User user) {
        if (user == null) {
            return null;
        }

        Profile profile = user.getProfile();
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .build();
    }

    public UserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }

        Profile profile = user.getProfile();
        return UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .isVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .build();
    }
}
