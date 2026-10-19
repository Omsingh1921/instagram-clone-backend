package com.instagramclone.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistServiceImpl implements ITokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);

    private final StringRedisTemplate stringRedisTemplate;

    // Resilient in-memory fallbacks if Redis is unreachable or during unit tests.
    // NOTE: In-memory fallback is scoped to this JVM instance and is NOT distributed across multiple nodes during Redis downtime.
    private final Map<String, Long> localBlacklist = new ConcurrentHashMap<>();
    private final Map<UUID, Long> localUserRevocation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> localSessionRevocation = new ConcurrentHashMap<>();

    @Autowired
    public TokenBlacklistServiceImpl(@Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void blacklistToken(String token, long ttlMillis) {
        if (token == null || token.isBlank()) {
            return;
        }

        long ttl = Math.max(1000L, ttlMillis);
        String tokenHash = hashToken(token.trim());
        String key = "blacklist:token:" + tokenHash;

        boolean savedToRedis = false;
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
                savedToRedis = true;
                log.info("Blacklisted JWT token in Redis with key: {} and TTL: {}ms", key, ttl);
            } catch (Exception ex) {
                log.warn("Redis unavailable during token blacklist write: {}. Using in-memory fallback.", ex.getMessage());
            }
        }

        if (!savedToRedis) {
            localBlacklist.put(tokenHash, System.currentTimeMillis() + ttl);
            log.info("Blacklisted JWT token in local memory fallback with hash: {}", tokenHash);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(token.trim());
        String key = "blacklist:token:" + tokenHash;

        if (stringRedisTemplate != null) {
            try {
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable during blacklist check for token {}: {}", tokenHash, ex.getMessage());
            }
        }

        Long expiry = localBlacklist.get(tokenHash);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry) {
                return true;
            } else {
                localBlacklist.remove(tokenHash);
            }
        }

        return false;
    }

    @Override
    public void blacklistUser(UUID userId, long durationMillis) {
        if (userId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = Math.max(1000L, durationMillis);
        String key = "blacklist:user:" + userId;

        boolean savedToRedis = false;
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(now), ttl, TimeUnit.MILLISECONDS);
                savedToRedis = true;
                log.info("Blacklisted all active tokens for user {} in Redis with TTL {}ms", userId, ttl);
            } catch (Exception ex) {
                log.warn("Redis unavailable during user blacklist write: {}. Using in-memory fallback.", ex.getMessage());
            }
        }

        if (!savedToRedis) {
            localUserRevocation.put(userId, now);
            log.info("Blacklisted all active tokens for user {} in local memory fallback", userId);
        }
    }

    @Override
    public boolean isUserRevoked(UUID userId, Date issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }

        long tokenIssuedEpoch = issuedAt.getTime();
        String key = "blacklist:user:" + userId;

        if (stringRedisTemplate != null) {
            try {
                String revokedEpochStr = stringRedisTemplate.opsForValue().get(key);
                if (revokedEpochStr != null) {
                    long revokedEpoch = Long.parseLong(revokedEpochStr);
                    // If token was issued at or before revocation, it is revoked
                    if (tokenIssuedEpoch <= revokedEpoch) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable during user revocation check for user {}: {}", userId, ex.getMessage());
            }
        }

        Long localRevokedEpoch = localUserRevocation.get(userId);
        if (localRevokedEpoch != null) {
            return tokenIssuedEpoch <= localRevokedEpoch;
        }

        return false;
    }

    @Override
    public void clearUserBlacklist(UUID userId) {
        if (userId == null) {
            return;
        }

        String key = "blacklist:user:" + userId;
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(key);
                log.info("Cleared user blacklist in Redis for user {}", userId);
            } catch (Exception ex) {
                log.warn("Redis unavailable during user blacklist clear: {}. Clearing local fallback.", ex.getMessage());
            }
        }
        localUserRevocation.remove(userId);
    }

    @Override
    public void blacklistSession(UUID sessionId, long durationMillis) {
        if (sessionId == null) {
            return;
        }

        long ttl = Math.max(1000L, durationMillis);
        String key = "blacklist:session:" + sessionId;

        boolean savedToRedis = false;
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
                savedToRedis = true;
                log.info("Blacklisted session [{}] in Redis with key: {} and TTL: {}ms", sessionId, key, ttl);
            } catch (Exception ex) {
                log.warn("Redis unavailable during session blacklist write: {}. Using in-memory fallback (local JVM only).", ex.getMessage());
            }
        }

        if (!savedToRedis) {
            localSessionRevocation.put(sessionId, System.currentTimeMillis() + ttl);
            log.info("Blacklisted session [{}] in local memory fallback", sessionId);
        }
    }

    @Override
    public boolean isSessionRevoked(UUID sessionId) {
        if (sessionId == null) {
            return false;
        }

        String key = "blacklist:session:" + sessionId;

        if (stringRedisTemplate != null) {
            try {
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Redis unavailable during session revocation check for session {}: {}", sessionId, ex.getMessage());
            }
        }

        Long expiry = localSessionRevocation.get(sessionId);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry) {
                return true;
            } else {
                localSessionRevocation.remove(sessionId);
            }
        }

        return false;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in runtime", e);
        }
    }
}
