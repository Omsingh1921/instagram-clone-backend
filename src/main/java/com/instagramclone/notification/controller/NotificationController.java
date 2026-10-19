package com.instagramclone.notification.controller;

import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.notification.constants.NotificationConstants;
import com.instagramclone.notification.dto.response.NotificationResponse;
import com.instagramclone.notification.dto.response.UnreadCountResponse;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(NotificationConstants.BASE_PATH)
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(name = "type", required = false) NotificationType type,
            @PageableDefault(size = NotificationConstants.DEFAULT_PAGE_SIZE, sort = NotificationConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<NotificationResponse> notifications = notificationService.getNotifications(type, pageable);
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_NOTIFICATIONS_RETRIEVED, notifications));
    }

    @GetMapping(NotificationConstants.UNREAD_COUNT_PATH)
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        UnreadCountResponse response = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_UNREAD_COUNT_RETRIEVED, response));
    }

    @PutMapping(NotificationConstants.NOTIFICATION_READ_PATH)
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_NOTIFICATION_READ, null));
    }

    @PutMapping(NotificationConstants.READ_ALL_PATH)
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_ALL_READ, null));
    }

    @DeleteMapping(NotificationConstants.NOTIFICATION_BY_ID_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_NOTIFICATION_DELETED, null));
    }

    @DeleteMapping(NotificationConstants.CLEAR_READ_PATH)
    public ResponseEntity<ApiResponse<Void>> clearReadNotifications() {
        notificationService.clearReadNotifications();
        return ResponseEntity.ok(ApiResponse.success(NotificationConstants.MSG_READ_NOTIFICATIONS_CLEARED, null));
    }
}
