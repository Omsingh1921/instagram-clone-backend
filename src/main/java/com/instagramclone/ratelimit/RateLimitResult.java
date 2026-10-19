package com.instagramclone.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private long currentCount;
    private long limit;
    private long remaining;
    private long retryAfterSeconds;
    private long resetSeconds;
}
