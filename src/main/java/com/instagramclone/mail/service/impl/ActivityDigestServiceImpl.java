package com.instagramclone.mail.service.impl;

import com.instagramclone.mail.service.interfaces.*;

import com.instagramclone.mail.dto.EmailCommand;
import com.instagramclone.mail.enums.EmailPriority;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.notification.entity.Notification;
import com.instagramclone.notification.repository.NotificationRepository;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.enums.DigestFrequency;
import com.instagramclone.user.repository.UserSettingsRepository;
import com.instagramclone.utility.RedisUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityDigestServiceImpl implements IActivityDigestService {

    private static final Logger log = LoggerFactory.getLogger(ActivityDigestServiceImpl.class);

    private final UserSettingsRepository userSettingsRepository;
    private final NotificationRepository notificationRepository;
    private final EmailDispatcher emailDispatcher;
    private final RedisUtility redisUtility;

    @Override
    @Scheduled(cron = "${app.mail.digest.daily-cron:0 0 9 * * *}") // Every day at 9:00 AM
    public int executeDailyActivityDigest() {
        return runWithDistributedLock(
                "lock:job:email:digest:daily",
                DigestFrequency.DAILY,
                LocalDateTime.now().minusDays(1),
                EmailType.DAILY_ACTIVITY_DIGEST
        );
    }

    @Override
    @Scheduled(cron = "${app.mail.digest.weekly-cron:0 0 9 * * MON}") // Every Monday at 9:00 AM
    public int executeWeeklyActivityDigest() {
        return runWithDistributedLock(
                "lock:job:email:digest:weekly",
                DigestFrequency.WEEKLY,
                LocalDateTime.now().minusDays(7),
                EmailType.WEEKLY_ACTIVITY_DIGEST
        );
    }

    private int runWithDistributedLock(String lockKey, DigestFrequency frequency, LocalDateTime windowStart, EmailType emailType) {
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = false;

        if (redisUtility.isAvailable()) {
            locked = redisUtility.acquireLock(lockKey, lockOwner, Duration.ofMinutes(15));
            if (!locked) {
                log.info("Activity digest job [{}] skipped: another instance holds distributed lock", frequency);
                return 0;
            }
        }

        try {
            log.info("Starting [{}] activity digest dispatch job (window start: {})", frequency, windowStart);
            return processDigestBatches(frequency, windowStart, emailType);
        } finally {
            if (locked && redisUtility.isAvailable()) {
                redisUtility.releaseLock(lockKey, lockOwner);
            }
        }
    }

    @Transactional(readOnly = true)
    protected int processDigestBatches(DigestFrequency frequency, LocalDateTime windowStart, EmailType emailType) {
        int dispatchedCount = 0;
        int page = 0;
        int pageSize = 50;
        boolean hasMore = true;

        while (hasMore) {
            Page<UserSettings> batch = userSettingsRepository
                    .findByEmailActivityDigestTrueAndDigestFrequency(frequency, PageRequest.of(page, pageSize));

            for (UserSettings settings : batch.getContent()) {
                User user = settings.getUser();
                if (user == null || user.getEmail() == null) {
                    continue;
                }

                // Deduplicate per user and calendar period
                String periodTag = frequency.isDaily()
                        ? LocalDate.now().toString()
                        : LocalDate.now().getYear() + "-W" + LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

                String debounceKey = "debounce:mail:DIGEST:" + frequency.name() + ":" + user.getId() + ":" + periodTag;
                if (redisUtility.isAvailable()) {
                    boolean isFirst = redisUtility.setIfAbsent(debounceKey, "1", Duration.ofDays(frequency.isDaily() ? 1 : 7));
                    if (!isFirst) {
                        continue;
                    }
                }

                try {
                    long unreadCount = notificationRepository
                            .countByRecipientIdAndReadFalseAndCreatedAtAfter(user.getId(), windowStart);

                    if (unreadCount <= 0) {
                        continue; // Do not send empty digest emails
                    }

                    List<Notification> samples = notificationRepository
                            .findByRecipientIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), windowStart, PageRequest.of(0, 3));

                    String summary = buildDigestSummary(samples, unreadCount, frequency);

                    EmailCommand command = EmailCommand.builder()
                            .emailType(emailType)
                            .recipientEmail(user.getEmail())
                            .recipientUserId(user.getId())
                            .priority(EmailPriority.LOW)
                            .variables(Map.of(
                                    "username", user.getUsername(),
                                    "unreadCount", unreadCount,
                                    "summary", summary,
                                    "frequency", frequency.name()
                            ))
                            .build();

                    emailDispatcher.dispatch(command);
                    dispatchedCount++;
                } catch (Exception ex) {
                    log.error("Failed dispatching [{}] activity digest to user [{}]: {}",
                            frequency, user.getUsername(), ex.getMessage());
                }
            }

            hasMore = batch.hasNext();
            page++;
        }

        log.info("Completed [{}] activity digest dispatch job. Successfully sent {} emails.",
                frequency, dispatchedCount);
        return dispatchedCount;
    }

    private String buildDigestSummary(List<Notification> samples, long totalUnread, DigestFrequency frequency) {
        StringBuilder sb = new StringBuilder();
        sb.append("You have ").append(totalUnread).append(" unread notifications in your ")
                .append(frequency.name().toLowerCase()).append(" digest:\n");

        for (Notification n : samples) {
            String actorName = (n.getActor() != null) ? n.getActor().getUsername() : "Someone";
            sb.append("- ").append(actorName).append(" ");
            if (n.getType() != null) {
                switch (n.getType()) {
                    case FOLLOW -> sb.append("started following you");
                    case POST_LIKED -> sb.append("liked your post");
                    case POST_COMMENTED -> sb.append("commented on your post");
                    case COMMENT_REPLIED -> sb.append("replied to your comment");
                    default -> sb.append("interacted with your profile");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
