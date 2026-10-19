package com.instagramclone.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String websiteUrl;
    @Builder.Default
    private Long postCount = 0L;
    @Builder.Default
    private Long followerCount = 0L;
    @Builder.Default
    private Long followingCount = 0L;
    private Boolean followedByCurrentUser;
    @Builder.Default
    private Boolean isPrivate = false;
    private Boolean followRequestedByCurrentUser;
    private LocalDateTime createdAt;
}
