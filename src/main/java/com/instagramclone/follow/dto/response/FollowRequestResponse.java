package com.instagramclone.follow.dto.response;

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
public class FollowRequestResponse {

    private UUID id;
    private UUID requesterId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
