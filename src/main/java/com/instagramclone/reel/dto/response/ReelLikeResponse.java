package com.instagramclone.reel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReelLikeResponse {
    private UUID reelId;
    private boolean liked;
    private long likeCount;
}
