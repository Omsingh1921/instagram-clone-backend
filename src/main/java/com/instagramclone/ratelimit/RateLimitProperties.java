package com.instagramclone.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int defaultLimit = 120;
    private int defaultWindowSeconds = 60;
    private java.util.List<String> trustedProxies = new java.util.ArrayList<>(java.util.List.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1"));
    private Map<String, CategoryLimit> categories = new HashMap<>();

    @Data
    public static class CategoryLimit {
        private int limit = 60;
        private int windowSeconds = 60;

        public CategoryLimit() {}

        public CategoryLimit(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }
    }

    public CategoryLimit getCategoryLimit(String category) {
        if (categories != null && categories.containsKey(category)) {
            return categories.get(category);
        }
        return new CategoryLimit(defaultLimit, defaultWindowSeconds);
    }
}
