package com.instagramclone.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends ApiException {

    private final long retryAfterSeconds;
    private final long limit;

    public RateLimitExceededException(String message, long retryAfterSeconds, long limit) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
    }
}
