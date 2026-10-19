package com.instagramclone.user.dto.response;

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
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String websiteUrl;
    private UserStatus status;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime createdAt;
}
