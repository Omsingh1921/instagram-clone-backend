package com.instagramclone.common.job;

import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.story.service.interfaces.IStoryService;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import com.instagramclone.auth.repository.SignupVerificationTicketRepository;
import com.instagramclone.utility.RedisDistributedLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduledCleanupJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupJobs.class);

    private final IStoryService storyService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SignupVerificationTicketRepository signupVerificationTicketRepository;
    private final RedisDistributedLockService lockService;

    @Scheduled(cron = "${app.jobs.story-cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredStories() {
        String lockKey = CacheConstants.CACHE_LOCK_JOB_PREFIX + "cleanup-expired-stories";
        String lockOwner = UUID.randomUUID().toString();
        Duration leaseDuration = Duration.ofMinutes(5);

        if (!lockService.tryLock(lockKey, lockOwner, leaseDuration)) {
            log.info("Another instance is currently running story cleanup. Skipping job.");
            return;
        }

        log.info("Acquired lock [{}]. Starting scheduled cleanup of expired stories...", lockKey);
        try {
            int cleaned = storyService.cleanupExpiredStories();
            log.info("Expired stories cleanup completed. Removed: {}", cleaned);
        } catch (Exception ex) {
            log.error("Failed to run scheduled story cleanup", ex);
        } finally {
            lockService.releaseLock(lockKey, lockOwner);
        }
    }

    @Scheduled(cron = "${app.jobs.token-cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        String lockKey = CacheConstants.CACHE_LOCK_JOB_PREFIX + "cleanup-expired-tokens";
        String lockOwner = UUID.randomUUID().toString();
        Duration leaseDuration = Duration.ofMinutes(10);

        if (!lockService.tryLock(lockKey, lockOwner, leaseDuration)) {
            log.info("Another instance is currently running token purge. Skipping job.");
            return;
        }

        log.info("Acquired lock [{}]. Starting scheduled purge of expired/revoked refresh tokens...", lockKey);
        try {
            int purged = refreshTokenRepository.purgeExpiredOrRevokedTokens(LocalDateTime.now());
            log.info("Token purge completed. Removed: {}", purged);
        } catch (Exception ex) {
            log.error("Failed to run scheduled token purge", ex);
        } finally {
            lockService.releaseLock(lockKey, lockOwner);
        }
    }

    @Scheduled(cron = "${app.jobs.signup-ticket-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredSignupTickets() {
        String lockKey = CacheConstants.CACHE_LOCK_JOB_PREFIX + "cleanup-signup-tickets";
        String lockOwner = UUID.randomUUID().toString();
        Duration leaseDuration = Duration.ofMinutes(10);

        if (!lockService.tryLock(lockKey, lockOwner, leaseDuration)) {
            log.info("Another instance is currently running signup ticket purge. Skipping job.");
            return;
        }

        log.info("Acquired lock [{}]. Starting scheduled purge of expired/consumed signup tickets...", lockKey);
        try {
            int purged = signupVerificationTicketRepository.purgeExpiredOrConsumedTickets(LocalDateTime.now());
            log.info("Signup ticket purge completed. Removed: {}", purged);
        } catch (Exception ex) {
            log.error("Failed to run scheduled signup ticket purge", ex);
        } finally {
            lockService.releaseLock(lockKey, lockOwner);
        }
    }
}
