package com.instagramclone.feed.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Cursor payload for keyset-based cursor pagination of the home feed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedCursor {

    private Instant timestamp;
    private UUID lastId;
    private boolean hasMore;
}
