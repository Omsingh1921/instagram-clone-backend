package com.instagramclone.engagement.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common representation of engagement statistics for an entity (Post or Reel).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementStats {

    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long bookmarksCount;
    private boolean isLikedByCurrentUser;
    private boolean isBookmarkedByCurrentUser;
}
