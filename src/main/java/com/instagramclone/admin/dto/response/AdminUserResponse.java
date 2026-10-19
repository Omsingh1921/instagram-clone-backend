package com.instagramclone.admin.dto.response;

import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
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
public class AdminUserResponse {

    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Role role;
    private UserStatus status;
    private Boolean isActive;
    private Boolean isDeleted;
    private Boolean emailVerified;
    private long postsCount;
    private long followersCount;
    private long followingCount;
    private LocalDateTime createdAt;
}
