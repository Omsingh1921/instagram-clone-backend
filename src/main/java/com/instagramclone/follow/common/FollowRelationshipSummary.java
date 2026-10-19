package com.instagramclone.follow.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Common representation of follow status between the current viewer and a target user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowRelationshipSummary {

    private UUID targetUserId;
    private boolean isFollowing;
    private boolean isFollowedBy;
    private boolean isPending;
    private boolean isBlocked;
    private boolean isMuted;
}
