package com.instagramclone.search.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Common representation of an indexed search item (user or hashtag).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {

    private String type; // "USER" or "HASHTAG"
    private UUID id;
    private String title;
    private String subtitle;
    private String avatarUrl;
    private long score;
}
