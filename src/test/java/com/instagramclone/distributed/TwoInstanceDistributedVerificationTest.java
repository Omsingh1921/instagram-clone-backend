package com.instagramclone.distributed;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.response.PresenceResponse;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.event.DomainEvent;
import com.instagramclone.common.event.DomainEventConsumer;
import com.instagramclone.ratelimit.RateLimitResult;
import com.instagramclone.ratelimit.RedisRateLimiterService;
import com.instagramclone.security.ITokenBlacklistService;
import com.instagramclone.utility.RedisDistributedLockService;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TwoInstanceDistributedVerificationTest extends BaseIntegrationTest {

    @Autowired
    private RedisRateLimiterService rateLimiterService;

    @Autowired
    private RedisDistributedLockService lockService;

    @Autowired
    private ITokenBlacklistService tokenBlacklistService;

    @Autowired
    private IPresenceService presenceService;

    @Autowired
    private DomainEventConsumer eventConsumer;

    @Autowired
    private RedisUtility redisUtility;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    @DisplayName("Two-Instance Verification: Distributed Rate Limiting accumulates across instances")
    void testDistributedRateLimitingAcrossInstances() {
        // Simulate two backend nodes (Instance A and Instance B) handling requests for the same client
        String sharedKey = "rate_limit:test-distributed:user:" + UUID.randomUUID();
        int limit = 5;
        int windowSeconds = 60;

        try {
            // Instance A receives 3 requests
            for (int i = 0; i < 3; i++) {
                RateLimitResult resA = rateLimiterService.checkRateLimit(sharedKey, limit, windowSeconds);
                assertThat(resA.isAllowed()).isTrue();
            }

            // Instance B receives 2 requests -> exhausts the shared limit of 5
            for (int i = 0; i < 2; i++) {
                RateLimitResult resB = rateLimiterService.checkRateLimit(sharedKey, limit, windowSeconds);
                assertThat(resB.isAllowed()).isTrue();
            }

            // Next request to Instance A MUST be blocked
            RateLimitResult blockedA = rateLimiterService.checkRateLimit(sharedKey, limit, windowSeconds);
            assertThat(blockedA.isAllowed()).isFalse();
            assertThat(blockedA.getRemaining()).isEqualTo(0);

            // Next request to Instance B MUST ALSO be blocked
            RateLimitResult blockedB = rateLimiterService.checkRateLimit(sharedKey, limit, windowSeconds);
            assertThat(blockedB.isAllowed()).isFalse();
            assertThat(blockedB.getRemaining()).isEqualTo(0);
        } finally {
            redisUtility.delete(sharedKey);
        }
    }

    @Test
    @DisplayName("Two-Instance Verification: Mutual exclusion and safe release between Node A and Node B")
    void testDistributedLockAcrossInstances() {
        String lockKey = "lock:job:cleanup-" + UUID.randomUUID();
        String nodeA = "instance-worker-node-A";
        String nodeB = "instance-worker-node-B";
        Duration lease = Duration.ofSeconds(15);

        try {
            // Node A acquires the lock
            boolean acquiredA = lockService.tryLock(lockKey, nodeA, lease);
            assertThat(acquiredA).isTrue();
            assertThat(lockService.isLockHeld(lockKey, nodeA)).isTrue();

            // Node B attempts to acquire the lock -> blocked
            boolean acquiredB = lockService.tryLock(lockKey, nodeB, lease);
            assertThat(acquiredB).isFalse();
            assertThat(lockService.isLockHeld(lockKey, nodeB)).isFalse();

            // Node B attempts rogue release -> fails
            boolean rogueRelease = lockService.releaseLock(lockKey, nodeB);
            assertThat(rogueRelease).isFalse();
            assertThat(lockService.isLockHeld(lockKey, nodeA)).isTrue();

            // Node A releases lock
            boolean releasedA = lockService.releaseLock(lockKey, nodeA);
            assertThat(releasedA).isTrue();

            // Now Node B can acquire
            boolean acquiredBAfterRelease = lockService.tryLock(lockKey, nodeB, lease);
            assertThat(acquiredBAfterRelease).isTrue();
            assertThat(lockService.isLockHeld(lockKey, nodeB)).isTrue();

            lockService.releaseLock(lockKey, nodeB);
        } finally {
            redisUtility.delete(lockKey);
        }
    }

    @Test
    @DisplayName("Two-Instance Verification: User session revocation propagated across instances via Redis")
    void testSessionRevocationAcrossInstances() {
        UUID userId = UUID.randomUUID();
        Date tokenIssuedTime = new Date(System.currentTimeMillis() - 5000); // 5 seconds ago

        // Instance B checks token -> not revoked yet
        boolean revokedBefore = tokenBlacklistService.isUserRevoked(userId, tokenIssuedTime);
        assertThat(revokedBefore).isFalse();

        // User changes password on Instance A -> revokes all tokens
        tokenBlacklistService.blacklistUser(userId, 600000L); // 10 minutes

        // Instance B checks the same token -> detected as REVOKED across the cluster
        boolean revokedAfter = tokenBlacklistService.isUserRevoked(userId, tokenIssuedTime);
        assertThat(revokedAfter).isTrue();

        // New token issued AFTER password change is NOT revoked
        Date newTokenIssuedTime = new Date(System.currentTimeMillis() + 1000);
        boolean newTokenRevoked = tokenBlacklistService.isUserRevoked(userId, newTokenIssuedTime);
        assertThat(newTokenRevoked).isFalse();
    }

    @Test
    @DisplayName("Two-Instance Verification: Multi-device WebSocket presence shared across instances")
    void testMultiDevicePresenceAcrossInstances() {
        UUID userId = UUID.randomUUID();
        String sessionNodeA = "ws-session-desktop-" + UUID.randomUUID();
        String sessionNodeB = "ws-session-mobile-" + UUID.randomUUID();

        // Device 1 connects to Node A
        presenceService.userConnected(userId, sessionNodeA);
        PresenceResponse presence1 = presenceService.getPresence(userId);
        assertThat(presence1.isOnline()).isTrue();

        // Device 2 connects to Node B
        presenceService.userConnected(userId, sessionNodeB);
        PresenceResponse presence2 = presenceService.getPresence(userId);
        assertThat(presence2.isOnline()).isTrue();

        // Device 1 disconnects on Node A -> user remains online because Node B session is still active
        presenceService.userDisconnected(userId, sessionNodeA);
        PresenceResponse presenceAfterA = presenceService.getPresence(userId);
        assertThat(presenceAfterA.isOnline()).isTrue();

        // Device 2 disconnects on Node B -> user is now offline
        presenceService.userDisconnected(userId, sessionNodeB);
        PresenceResponse presenceAfterB = presenceService.getPresence(userId);
        assertThat(presenceAfterB.isOnline()).isFalse();
        assertThat(presenceAfterB.getLastSeen()).isNotNull();
    }

    @Test
    @DisplayName("Two-Instance Verification: Kafka domain event deduplicated between Node A and Node B")
    void testKafkaDeduplicationAcrossInstances() {
        String eventId = UUID.randomUUID().toString();
        DomainEvent event = DomainEvent.builder()
                .eventId(eventId)
                .eventType("POST_LIKED")
                .actorId(UUID.randomUUID())
                .targetId(UUID.randomUUID())
                .build();

        String key = CacheConstants.CACHE_IDEMPOTENCY_EVENT_PREFIX + eventId;
        try {
            // Node A processes the event
            eventConsumer.onKafkaEvent(event);
            assertThat(redisUtility.get(key)).isPresent().contains("processed");

            // Node B receives a duplicate delivery of the same event -> deduplicated safely
            eventConsumer.onKafkaEvent(event);
            assertThat(redisUtility.get(key)).isPresent().contains("processed");
        } finally {
            redisUtility.delete(key);
        }
    }
}
