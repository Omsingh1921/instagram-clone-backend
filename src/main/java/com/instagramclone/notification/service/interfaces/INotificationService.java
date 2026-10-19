package com.instagramclone.notification.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.notification.dto.response.NotificationResponse;
import com.instagramclone.notification.dto.response.UnreadCountResponse;
import com.instagramclone.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface INotificationService {

    PageResponse<NotificationResponse> getNotifications(Pageable pageable);

    PageResponse<NotificationResponse> getNotifications(NotificationType type, Pageable pageable);

    UnreadCountResponse getUnreadCount();

    void markAsRead(UUID notificationId);

    void markAllAsRead();

    void deleteNotification(UUID notificationId);

    void clearReadNotifications();

    void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId);

    void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId, UUID reelId);

    void createNotification(UUID recipientId, UUID actorId, NotificationType type, UUID postId, UUID commentId, UUID reelId, String mediaPreviewUrl);
}
