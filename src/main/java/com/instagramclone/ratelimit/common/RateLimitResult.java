package com.instagramclone.ratelimit.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common payload representing the outcome of a rate-limiting check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    private boolean allowed;
    private long remainingRequests;
    private long retryAfterSeconds;
}
