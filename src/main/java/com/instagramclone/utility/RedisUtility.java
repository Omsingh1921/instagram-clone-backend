package com.instagramclone.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Low-level Redis key formatting and technical helpers adhering to Section 49.8 (Redis Utility).
 * Encapsulates safe key creation, TTL handling, and common cache operations.
 * Does NOT contain domain/business logic.
 */
@Component
public class RedisUtility {

    private static final Logger log = LoggerFactory.getLogger(RedisUtility.class);

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisUtility(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static String buildKey(String prefix, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object part : parts) {
            if (part != null) {
                sb.append(":").append(part.toString().trim());
            }
        }
        return sb.toString();
    }

    public boolean isAvailable() {
        return redisTemplate != null;
    }

    public Optional<String> get(String key) {
        if (redisTemplate == null || key == null) return Optional.empty();
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception ex) {
            log.warn("Redis GET failed for key '{}': {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void set(String key, String value, Duration ttl) {
        if (redisTemplate == null || key == null || value == null) return;
        try {
            if (ttl != null) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
        } catch (Exception ex) {
            log.warn("Redis SET failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public void delete(String key) {
        if (redisTemplate == null || key == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Redis DELETE failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public void deleteAfterCommit(String... keys) {
        if (keys == null || keys.length == 0) return;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            for (String key : keys) {
                                delete(key);
                            }
                        }
                    }
            );
        } else {
            for (String key : keys) {
                delete(key);
            }
        }
    }

    public boolean hasKey(String key) {
        if (redisTemplate == null || key == null) return false;
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.warn("Redis HAS_KEY failed for key '{}': {}", key, ex.getMessage());
            return false;
        }
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        if (redisTemplate == null || key == null || value == null) return false;
        try {
            Boolean success = (ttl != null)
                    ? redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
                    : redisTemplate.opsForValue().setIfAbsent(key, value);
            return Boolean.TRUE.equals(success);
        } catch (Exception ex) {
            log.warn("Redis SET_IF_ABSENT failed for key '{}': {}", key, ex.getMessage());
            return false;
        }
    }

    public boolean acquireLock(String lockKey, String lockOwner, Duration leaseDuration) {
        return setIfAbsent(lockKey, lockOwner, leaseDuration);
    }

    public boolean releaseLock(String lockKey, String lockOwner) {
        if (redisTemplate == null || lockKey == null || lockOwner == null) return false;
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
                "    return redis.call('del', KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        try {
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> script =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
            Long result = redisTemplate.execute(script, java.util.Collections.singletonList(lockKey), lockOwner);
            return result != null && result > 0;
        } catch (Exception ex) {
            log.warn("Redis RELEASE_LOCK failed for key '{}': {}", lockKey, ex.getMessage());
            return false;
        }
    }

    public boolean executeLua(String luaScript, String key, String... args) {
        if (redisTemplate == null || key == null || luaScript == null) return false;
        try {
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> script =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class);
            Long result = redisTemplate.execute(script, java.util.Collections.singletonList(key), (Object[]) args);
            return result != null && result > 0;
        } catch (Exception ex) {
            log.warn("Redis Lua execution failed for key '{}': {}", key, ex.getMessage());
            return false;
        }
    }

    public void sAdd(String key, String value, Duration ttl) {
        if (redisTemplate == null || key == null || value == null) return;
        try {
            redisTemplate.opsForSet().add(key, value);
            if (ttl != null) {
                redisTemplate.expire(key, ttl);
            }
        } catch (Exception ex) {
            log.warn("Redis SADD failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public void sRem(String key, String value) {
        if (redisTemplate == null || key == null || value == null) return;
        try {
            redisTemplate.opsForSet().remove(key, value);
        } catch (Exception ex) {
            log.warn("Redis SREM failed for key '{}': {}", key, ex.getMessage());
        }
    }

    public Long sCard(String key) {
        if (redisTemplate == null || key == null) return 0L;
        try {
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0L;
        } catch (Exception ex) {
            log.warn("Redis SCARD failed for key '{}': {}", key, ex.getMessage());
            return 0L;
        }
    }

    public java.util.Set<String> sMembers(String key) {
        if (redisTemplate == null || key == null) return java.util.Collections.emptySet();
        try {
            java.util.Set<String> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : java.util.Collections.emptySet();
        } catch (Exception ex) {
            log.warn("Redis SMEMBERS failed for key '{}': {}", key, ex.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    public Long increment(String key) {
        if (redisTemplate == null || key == null) return null;
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception ex) {
            log.warn("Redis INCR failed for key '{}': {}", key, ex.getMessage());
            return null;
        }
    }

    public Long decrement(String key) {
        if (redisTemplate == null || key == null) return null;
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception ex) {
            log.warn("Redis DECR failed for key '{}': {}", key, ex.getMessage());
            return null;
        }
    }
}
