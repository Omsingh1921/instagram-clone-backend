package com.instagramclone.ratelimit;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RateLimitingIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Should enforce rate limit and return 429 when threshold exceeded")
    void testRateLimitEnforcement() throws Exception {
        String testIp = "192.168.99.10";

        // Clean up test key if present
        if (redisTemplate != null) {
            redisTemplate.delete("rate_limit:auth-register:ip:" + testIp);
        }

        RegisterRequest request = RegisterRequest.builder()
                .username("rl_user")
                .email("invalid_email") // triggers validation, but rate limiter runs first
                .password("short")
                .build();

        // auth-register has a limit of 5 requests per 60s
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"))
                    .andExpect(status().isBadRequest()); // 400 validation error, NOT 429
        }

        // 6th request must exceed limit and return 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Should isolate rate limits between different client IPs")
    void testRateLimitIpIsolation() throws Exception {
        String ipA = "10.0.0.1";
        String ipB = "10.0.0.2";

        if (redisTemplate != null) {
            redisTemplate.delete("rate_limit:auth-register:ip:" + ipA);
            redisTemplate.delete("rate_limit:auth-register:ip:" + ipB);
        }

        RegisterRequest request = RegisterRequest.builder()
                .username("rl_user")
                .email("bad_email")
                .password("short")
                .build();

        // Exhaust IP A
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .header("X-Forwarded-For", ipA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        // IP A is now rate-limited
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());

        // IP B must NOT be blocked
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", ipB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    @DisplayName("Untrusted remote peer cannot bypass rate limits by rotating spoofed X-Forwarded-For headers")
    void untrustedRemotePeer_CannotBypassRateLimit_BySpoofingHeaders() throws Exception {
        String untrustedPeerIp = "198.51.100.42";

        if (redisTemplate != null) {
            redisTemplate.delete("rate_limit:auth-register:ip:" + untrustedPeerIp);
        }

        RegisterRequest request = RegisterRequest.builder()
                .username("untrusted_probe")
                .email("bad_email")
                .password("short")
                .build();

        // Send 5 requests from the untrusted peer socket, rotating spoofed X-Forwarded-For on each request
        for (int i = 1; i <= 5; i++) {
            String spoofedHeader = "203.0.113." + i;
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(req -> {
                                req.setRemoteAddr(untrustedPeerIp);
                                return req;
                            })
                            .header("X-Forwarded-For", spoofedHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        // 6th request with yet another spoofed IP must be REJECTED (429) because rate limiting tracks the actual socket peer
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(untrustedPeerIp);
                            return req;
                        })
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errors[0]").value("RATE_LIMIT_EXCEEDED"));
    }
}
