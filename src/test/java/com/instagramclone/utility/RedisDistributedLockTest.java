package com.instagramclone.utility;

import com.instagramclone.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisDistributedLockTest extends BaseIntegrationTest {

    @Autowired
    private RedisDistributedLockService lockService;

    @Autowired
    private RedisUtility redisUtility;

    @Test
    @DisplayName("Distributed Lock: Mutual exclusion and safe release semantics")
    void testLockAcquisitionAndMutualExclusion() {
        String lockKey = "lock:job:test-" + UUID.randomUUID();
        String owner1 = "node-1-" + UUID.randomUUID();
        String owner2 = "node-2-" + UUID.randomUUID();
        Duration lease = Duration.ofSeconds(10);

        try {
            // 1. Owner 1 acquires lock
            boolean acquired1 = lockService.tryLock(lockKey, owner1, lease);
            assertThat(acquired1).isTrue();

            // 2. Owner 2 tries to acquire the same lock -> must fail
            boolean acquired2 = lockService.tryLock(lockKey, owner2, lease);
            assertThat(acquired2).isFalse();

            // 3. Owner 2 attempts to release Owner 1's lock -> must fail (safe release semantics)
            boolean rogueRelease = lockService.releaseLock(lockKey, owner2);
            assertThat(rogueRelease).isFalse();

            // 4. Owner 1 releases their lock -> must succeed
            boolean legitimateRelease = lockService.releaseLock(lockKey, owner1);
            assertThat(legitimateRelease).isTrue();

            // 5. Now Owner 2 can acquire the lock
            boolean acquiredAfterRelease = lockService.tryLock(lockKey, owner2, lease);
            assertThat(acquiredAfterRelease).isTrue();

            // Clean up
            lockService.releaseLock(lockKey, owner2);
        } finally {
            redisUtility.delete(lockKey);
        }
    }

    @Test
    @DisplayName("Distributed Lock: Automatic expiration after lease duration")
    void testLockLeaseExpiration() throws InterruptedException {
        String lockKey = "lock:job:expiry-" + UUID.randomUUID();
        String owner1 = "node-1";
        String owner2 = "node-2";
        Duration shortLease = Duration.ofMillis(300);

        try {
            boolean acquired = lockService.tryLock(lockKey, owner1, shortLease);
            assertThat(acquired).isTrue();

            // Immediately, owner 2 cannot acquire
            assertThat(lockService.tryLock(lockKey, owner2, Duration.ofSeconds(5))).isFalse();

            // Wait for lease to expire
            Thread.sleep(450);

            // Now owner 2 can acquire the expired lock
            boolean acquiredAfterExpiry = lockService.tryLock(lockKey, owner2, Duration.ofSeconds(5));
            assertThat(acquiredAfterExpiry).isTrue();

            lockService.releaseLock(lockKey, owner2);
        } finally {
            redisUtility.delete(lockKey);
        }
    }

    @Test
    @DisplayName("Distributed Lock: Gracefully handle null inputs")
    void testNullInputs() {
        assertThat(lockService.tryLock(null, "owner", Duration.ofSeconds(1))).isFalse();
        assertThat(lockService.tryLock("key", null, Duration.ofSeconds(1))).isFalse();
        assertThat(lockService.tryLock("key", "owner", null)).isFalse();
        assertThat(lockService.releaseLock(null, "owner")).isFalse();
        assertThat(lockService.releaseLock("key", null)).isFalse();
    }
}
