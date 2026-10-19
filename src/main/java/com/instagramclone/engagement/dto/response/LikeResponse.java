package com.instagramclone.engagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private UUID postId;
    private boolean liked;
    private long likeCount;
}
