package com.instagramclone.distributed;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.chat.dto.response.PresenceResponse;
import com.instagramclone.chat.service.interfaces.IPresenceService;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.common.event.DomainEvent;
import com.instagramclone.common.event.DomainEventConsumer;
import com.instagramclone.utility.RedisDistributedLockService;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedisFailureAndConsistencyTest extends BaseIntegrationTest {

    @Autowired
    private RedisDistributedLockService lockService;

    @Autowired
    private IPresenceService presenceService;

    @Autowired
    private DomainEventConsumer eventConsumer;

    @Autowired
    private RedisUtility redisUtility;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Failure & Recovery: Lock lease expiry allows crash recovery, safe release blocks expired worker")
    void testLockLeaseExpiryCrashRecoveryAndSafeRelease() throws InterruptedException {
        String lockKey = "lock:job:crash-" + UUID.randomUUID();
        String node1 = "crashed-worker-" + UUID.randomUUID();
        String node2 = "replacement-worker-" + UUID.randomUUID();
        Duration shortLease = Duration.ofMillis(300);

        try {
            // Node 1 acquires lock
            boolean acquired1 = lockService.tryLock(lockKey, node1, shortLease);
            assertThat(acquired1).isTrue();

            // Simulate Node 1 freezing or taking longer than lease (450ms > 300ms)
            Thread.sleep(450);

            // Node 1's lease has expired. Node 2 recovers and acquires the lock
            boolean acquired2 = lockService.tryLock(lockKey, node2, Duration.ofSeconds(10));
            assertThat(acquired2).isTrue();

            // Node 1 wakes up and attempts to release the lock -> Lua script MUST reject release
            boolean rogueRelease = lockService.releaseLock(lockKey, node1);
            assertThat(rogueRelease).isFalse();

            // Node 1 checks if it still holds the lock -> returns false, allowing worker to abort
            assertThat(lockService.isLockHeld(lockKey, node1)).isFalse();

            // Node 2 still holds the lock safely
            assertThat(lockService.isLockHeld(lockKey, node2)).isTrue();

            // Node 2 cleanly releases
            assertThat(lockService.releaseLock(lockKey, node2)).isTrue();
        } finally {
            redisUtility.delete(lockKey);
        }
    }

    @Test
    @DisplayName("Failure & Recovery: Lock renewal extends lease only for valid owner")
    void testLockRenewal() throws InterruptedException {
        String lockKey = "lock:job:renew-" + UUID.randomUUID();
        String owner = "worker-" + UUID.randomUUID();
        String intruder = "intruder-" + UUID.randomUUID();

        try {
            boolean acquired = lockService.tryLock(lockKey, owner, Duration.ofMillis(400));
            assertThat(acquired).isTrue();

            // Intruder tries to renew -> fails
            boolean intruderRenew = lockService.renewLock(lockKey, intruder, Duration.ofSeconds(5));
            assertThat(intruderRenew).isFalse();

            // Valid owner renews lock for 2 seconds
            boolean ownerRenew = lockService.renewLock(lockKey, owner, Duration.ofSeconds(2));
            assertThat(ownerRenew).isTrue();

            // Wait 500ms (original lease of 400ms would have expired)
            Thread.sleep(500);

            // Lock is still held by owner because of renewal
            assertThat(lockService.isLockHeld(lockKey, owner)).isTrue();

            lockService.releaseLock(lockKey, owner);
        } finally {
            redisUtility.delete(lockKey);
        }
    }

    @Test
    @DisplayName("Presence Recovery: Heartbeat refreshes lease, expired session is pruned automatically")
    void testPresenceHeartbeatAndStaleSessionPruning() {
        UUID userId = UUID.randomUUID();
        String sessionId = "session-ephemeral-" + UUID.randomUUID();

        // 1. Connect
        presenceService.userConnected(userId, sessionId);
        assertThat(presenceService.getPresence(userId).isOnline()).isTrue();

        // 2. Heartbeat refreshes
        presenceService.heartbeat(userId, sessionId);
        assertThat(presenceService.getPresence(userId).isOnline()).isTrue();

        // 3. Simulate sudden ungraceful disconnect / session lease expiration in Redis
        redisUtility.delete("presence:session:" + sessionId);

        // 4. Checking presence or running cleanup automatically prunes dead session from set
        PresenceResponse response = presenceService.getPresence(userId);
        assertThat(response.isOnline()).isFalse();
        assertThat(response.getLastSeen()).isNotNull();

        // Verify Set is now empty in Redis
        String sessionsKey = CacheConstants.CACHE_PRESENCE_USER_PREFIX + userId + ":sessions";
        assertThat(redisUtility.sCard(sessionsKey)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Consistency: deleteAfterCommit evicts cache on commit and preserves cache on rollback")
    void testDeleteAfterCommitRollbackSafety() {
        String testCacheKey = "cache:test:tx:" + UUID.randomUUID();
        redisUtility.set(testCacheKey, "cached_data", Duration.ofMinutes(5));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Scenario 1: Rolled-back transaction must NOT evict cache
        try {
            tx.execute(status -> {
                redisUtility.deleteAfterCommit(testCacheKey);
                throw new RuntimeException("Simulated DB failure triggering rollback");
            });
        } catch (Exception ignored) {}

        // Cache key MUST STILL EXIST because transaction rolled back
        Optional<String> afterRollback = redisUtility.get(testCacheKey);
        assertThat(afterRollback).isPresent().contains("cached_data");

        // Scenario 2: Committed transaction MUST evict cache
        tx.execute(status -> {
            redisUtility.deleteAfterCommit(testCacheKey);
            return true;
        });

        // Cache key MUST BE EVICTED now that transaction has committed
        Optional<String> afterCommit = redisUtility.get(testCacheKey);
        assertThat(afterCommit).isEmpty();
    }

    @Test
    @DisplayName("Kafka Idempotency: Failed event processing clears lock allowing redelivery retry")
    void testKafkaFailureClearsIdempotencyKey() {
        String eventId = UUID.randomUUID().toString();
        String idempotencyKey = CacheConstants.CACHE_IDEMPOTENCY_EVENT_PREFIX + eventId;

        DomainEvent badEvent = DomainEvent.builder()
                .eventId(eventId)
                .eventType("CRASH_EVENT")
                .build();

        // Subclass consumer that simulates failure during event processing
        DomainEventConsumer failingConsumer = new DomainEventConsumer(redisUtility) {
            @Override
            protected void processKafkaEvent(DomainEvent event) {
                throw new IllegalStateException("Simulated consumer DB deadlock / exception");
            }
        };

        // Execution must fail and throw exception
        assertThatThrownBy(() -> failingConsumer.onKafkaEvent(badEvent))
                .isInstanceOf(IllegalStateException.class);

        // The idempotency key must be CLEARED in Redis to allow Kafka consumer redelivery
        Optional<String> keyState = redisUtility.get(idempotencyKey);
        assertThat(keyState).isEmpty();

        // Now test standard consumer succeeding on redelivery
        eventConsumer.onKafkaEvent(badEvent);
        Optional<String> retryState = redisUtility.get(idempotencyKey);
        assertThat(retryState).isPresent().contains("processed");

        redisUtility.delete(idempotencyKey);
    }
}
