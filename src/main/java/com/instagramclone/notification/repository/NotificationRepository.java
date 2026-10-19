package com.instagramclone.notification.repository;

import com.instagramclone.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(UUID recipientId, com.instagramclone.notification.enums.NotificationType type, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    long countByRecipientIdAndReadFalseAndCreatedAtAfter(UUID recipientId, java.time.LocalDateTime after);

    java.util.List<Notification> findByRecipientIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID recipientId, java.time.LocalDateTime after, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllAsReadForRecipient(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.read = true")
    int deleteReadNotificationsForRecipient(@Param("recipientId") UUID recipientId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.recipient.id = :recipientId " +
           "AND n.actor.id = :actorId AND n.type = :type AND n.read = false " +
           "AND ((:postId IS NULL AND n.postId IS NULL) OR n.postId = :postId) " +
           "AND ((:commentId IS NULL AND n.commentId IS NULL) OR n.commentId = :commentId) " +
           "AND ((:reelId IS NULL AND n.reelId IS NULL) OR n.reelId = :reelId)")
    boolean existsUnreadNotification(
            @Param("recipientId") UUID recipientId,
            @Param("actorId") UUID actorId,
            @Param("type") com.instagramclone.notification.enums.NotificationType type,
            @Param("postId") UUID postId,
            @Param("commentId") UUID commentId,
            @Param("reelId") UUID reelId
    );
}
