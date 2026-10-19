package com.instagramclone.notification.service.impl;

import com.instagramclone.notification.service.interfaces.*;

import java.util.UUID;
import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.notification.constants.NotificationConstants;
import com.instagramclone.notification.dto.response.NotificationActorResponse;
import com.instagramclone.notification.dto.response.NotificationResponse;
import com.instagramclone.notification.dto.response.UnreadCountResponse;
import com.instagramclone.notification.entity.Notification;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.repository.NotificationRepository;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.instagramclone.utility.RedisUtility;
import com.instagramclone.common.constants.CacheConstants;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventPublisher;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.utility.RedisDistributedLockService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String UNREAD_CACHE_PREFIX = CacheConstants.CACHE_UNREAD_NOTIFICATIONS_PREFIX;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailEventPublisher emailEventPublisher;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    private final RedisUtility redisUtility;
    private final RedisDistributedLockService lockService;
    private final PlatformTransactionManager transactionManager;

    private boolean acquireLockWithRetry(String lockKey, String lockOwner, Duration lease, int maxRetries, long sleepMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (lockService.tryLock(lockKey, lockOwner, lease)) {
                return true;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Pageable pageable) {
        return getNotifications(null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(NotificationType type, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<Notification> page = (type == null)
                ? notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageable)
                : notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(currentUserId, type, pageable);
        return PageResponse.from(page.map(this::toNotificationResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        String cacheKey = UNREAD_CACHE_PREFIX + currentUserId;

        Optional<String> cached = redisUtility.get(cacheKey);
        if (cached.isPresent()) {
            return UnreadCountResponse.builder().unreadCount(Long.parseLong(cached.get())).build();
        }

        long unread = notificationRepository.countByRecipientIdAndReadFalse(currentUserId);

        redisUtility.set(cacheKey, String.valueOf(unread), Duration.ofMinutes(5));

        return UnreadCountResponse.builder().unreadCount(unread).build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(NotificationConstants.ERR_NOTIFICATION_NOT_FOUND + notificationId));

        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new ForbiddenException(NotificationConstants.ERR_UNAUTHORIZED_MARK_READ);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        invalidateUnreadCache(currentUserId);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        notificationRepository.markAllAsReadForRecipient(currentUserId);
        invalidateUnreadCache(currentUserId);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(NotificationConstants.ERR_NOTIFICATION_NOT_FOUND + notificationId));

        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new ForbiddenException(NotificationConstants.ERR_UNAUTHORIZED_DELETE_NOTIFICATION);
        }

        notificationRepository.delete(notification);
        invalidateUnreadCache(currentUserId);
    }

    @Override
    @Transactional
    public void clearReadNotifications() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        notificationRepository.deleteReadNotificationsForRecipient(currentUserId);
        invalidateUnreadCache(currentUserId);
    }

    @Override
    @Transactional
    public void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId) {
        createNotification(recipientId, actorId, type, postId, commentId, null, null);
    }

    @Override
    public void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId, UUID reelId) {
        createNotification(recipientId, actorId, type, postId, commentId, reelId, null);
    }

    @Override
    public void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId, UUID reelId, String mediaPreviewUrl) {
        if (recipientId == null || actorId == null || recipientId.equals(actorId)) {
            // Do not notify user about their own actions
            return;
        }

        String targetKey = (reelId != null) ? reelId.toString() :
                ((postId != null) ? postId.toString() :
                        ((commentId != null) ? commentId.toString() : "none"));
        String lockKey = "lock:notif:" + recipientId + ":" + actorId + ":" + type + ":" + targetKey;
        String lockOwner = UUID.randomUUID().toString();
        boolean locked = acquireLockWithRetry(lockKey, lockOwner, Duration.ofSeconds(5), 10, 50);

        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.executeWithoutResult(status ->
                    executeCreateNotification(recipientId, actorId, type, postId, commentId, reelId, mediaPreviewUrl)
            );
        } finally {
            if (locked) {
                lockService.releaseLock(lockKey, lockOwner);
            }
        }
    }

    private void executeCreateNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId, UUID reelId, String mediaPreviewUrl) {
        // Deduplication check: Do not create duplicate unread notifications for the same logical event
        if (notificationRepository.existsUnreadNotification(recipientId, actorId, type, postId, commentId, reelId)) {
            log.debug("Duplicate unread notification suppressed: recipient={}, actor={}, type={}, postId={}, commentId={}, reelId={}",
                    recipientId, actorId, type, postId, commentId, reelId);
            return;
        }

        User recipient = userRepository.findById(recipientId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);

        if (recipient == null || actor == null) {
            log.warn("Cannot create notification: recipient or actor missing. recipient={}, actor={}", recipientId, actorId);
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .postId(postId)
                .commentId(commentId)
                .reelId(reelId)
                .mediaPreviewUrl(mediaPreviewUrl)
                .read(false)
                .build();

        notificationRepository.save(notification);
        invalidateUnreadCache(recipientId);

        // Real-time delivery over WebSocket (Section 12)
        if (messagingTemplate != null) {
            try {
                NotificationResponse responseDto = toNotificationResponse(notification);
                messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, responseDto);
                log.debug("Dispatched real-time notification to /topic/notifications/{}", recipientId);
            } catch (Exception ex) {
                log.error("Failed to dispatch real-time WebSocket notification: {}", ex.getMessage());
            }
        }

        log.info("Notification {} created of type {} for user {}", notification.getId(), type, recipient.getUsername());

        // Dispatch purpose-based email event for social activities
        EmailType emailType = switch (type) {
            case FOLLOW -> EmailType.NEW_FOLLOWER;
            case POST_LIKED, REEL_LIKED -> EmailType.POST_LIKED;
            case POST_COMMENTED, COMMENT_REPLIED, REEL_COMMENTED -> EmailType.POST_COMMENTED;
            default -> null;
        };

        if (emailType != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("recipientUsername", recipient.getUsername());
            vars.put("actorUsername", actor.getUsername());
            vars.put("actorId", actor.getId());
            if (postId != null) vars.put("postId", postId);
            if (commentId != null) vars.put("commentId", commentId);
            if (reelId != null) vars.put("reelId", reelId);

            emailEventPublisher.publishAfterCommit(EmailEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("EMAIL_" + emailType.name())
                    .emailType(emailType)
                    .userId(recipient.getId())
                    .email(recipient.getEmail())
                    .username(recipient.getUsername())
                    .priority(emailType.getDefaultPriority())
                    .variables(vars)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    private void invalidateUnreadCache(UUID userId) {
        redisUtility.deleteAfterCommit(UNREAD_CACHE_PREFIX + userId);
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        User a = n.getActor();
        return NotificationResponse.builder()
                .id(n.getId())
                .actor(NotificationActorResponse.builder()
                        .id(a.getId())
                        .username(a.getUsername())
                        .displayName(a.getProfile() != null ? a.getProfile().getDisplayName() : null)
                        .avatarUrl(a.getProfile() != null ? a.getProfile().getAvatarUrl() : null)
                        .build())
                .type(n.getType())
                .postId(n.getPostId())
                .commentId(n.getCommentId())
                .reelId(n.getReelId())
                .mediaPreviewUrl(n.getMediaPreviewUrl())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
