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
public class CloseFriendResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime addedAt;
}
