package com.instagramclone.post.common;

import com.instagramclone.post.enums.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common lightweight representation of a Post across feed and search modules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSummary {

    private UUID id;
    private UUID authorId;
    private String authorUsername;
    private String caption;
    private String firstMediaUrl;
    private int mediaCount;
    private PostVisibility visibility;
    private long likesCount;
    private long commentsCount;
    private Instant createdAt;
}
