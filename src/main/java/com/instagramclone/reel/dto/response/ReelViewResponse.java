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
public class ReelViewResponse {
    private UUID reelId;
    private long viewCount;
    private boolean viewed;
}
