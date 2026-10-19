package com.instagramclone.follow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserResponse {

    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private Boolean following;
    private Boolean followedByCurrentUser;
}
