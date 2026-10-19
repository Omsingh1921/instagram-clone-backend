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
public class FollowResponse {

    private UUID followingId;
    private boolean following;
    @Builder.Default
    private boolean requested = false;
    private long followerCount;
    private long followingCount;
}
