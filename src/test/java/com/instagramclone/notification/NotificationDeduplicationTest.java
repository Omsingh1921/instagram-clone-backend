package com.instagramclone.notification;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.notification.entity.Notification;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.enums.PostVisibility;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeduplicationTest extends BaseIntegrationTest {

    @Autowired
    private INotificationService notificationService;

    private User recipient;
    private User actor;
    private Post post1;
    private Post post2;
    private Reel reel1;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        recipient = userRepository.save(User.builder()
                .username("notif_recipient")
                .email("recipient@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        actor = userRepository.save(User.builder()
                .username("notif_actor")
                .email("actor@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        post1 = postRepository.save(Post.builder()
                .user(recipient)
                .caption("First Post")
                .visibility(PostVisibility.PUBLIC)
                .build());

        post2 = postRepository.save(Post.builder()
                .user(recipient)
                .caption("Second Post")
                .visibility(PostVisibility.PUBLIC)
                .build());

        reel1 = reelRepository.save(Reel.builder()
                .user(recipient)
                .videoUrl("https://cloudinary.com/test/reel1.mp4")
                .caption("First Reel")
                .durationMs(12000L)
                .build());
    }

    @Test
    @DisplayName("Deduplication - Repeated service calls or Kafka retries for same unread event are suppressed")
    void repeatedUnreadNotification_ShouldBeDeduplicated() {
        // First notification creation
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post1.getId(), null);

        List<Notification> notifs = notificationRepository.findAll();
        assertThat(notifs).hasSize(1);
        assertThat(notifs.get(0).getType()).isEqualTo(NotificationType.POST_LIKED);
        assertThat(notifs.get(0).getPostId()).isEqualTo(post1.getId());
        assertThat(notifs.get(0).isRead()).isFalse();

        // Duplicate call (simulating Kafka redelivery or repeated trigger)
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post1.getId(), null);

        // Verification: count must still be 1
        assertThat(notificationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Legitimate New Events - New notification is delivered once the previous notification was marked as read")
    void newNotification_ShouldBeCreated_WhenPreviousWasRead() {
        // Initial like
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post1.getId(), null);
        assertThat(notificationRepository.findAll()).hasSize(1);

        // Mark the first notification as read
        Notification firstNotif = notificationRepository.findAll().get(0);
        firstNotif.setRead(true);
        notificationRepository.save(firstNotif);

        // Subsequent like notification (e.g. user unliked and liked again later)
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post1.getId(), null);

        // Verification: 2 notifications now exist (one read, one unread)
        List<Notification> allNotifs = notificationRepository.findAll();
        assertThat(allNotifs).hasSize(2);
        long unreadCount = allNotifs.stream().filter(n -> !n.isRead()).count();
        long readCount = allNotifs.stream().filter(Notification::isRead).count();
        assertThat(unreadCount).isEqualTo(1L);
        assertThat(readCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("Target Discrimination - Legitimate distinct events for same actor and recipient are preserved")
    void distinctEvents_ShouldNotBeSuppressed() {
        // Actor likes post 1
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post1.getId(), null);

        // Actor likes post 2
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.POST_LIKED, post2.getId(), null);

        // Actor likes reel 1
        notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.REEL_LIKED, null, null, reel1.getId());

        // Verification: all 3 distinct events exist
        List<Notification> allNotifs = notificationRepository.findAll();
        assertThat(allNotifs).hasSize(3);
    }

    @Test
    @DisplayName("Self Action - Actions performed on one's own content do not generate notifications")
    void selfAction_ShouldNotGenerateNotification() {
        notificationService.createNotification(recipient.getId(), recipient.getId(), NotificationType.POST_LIKED, post1.getId(), null);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Concurrency Audit - Concurrent duplicate notification events and Kafka redeliveries create exactly one notification")
    void concurrentDuplicateNotificationEvents_ShouldCreateExactlyOneRow() throws Exception {
        int threads = 6;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch finishLatch = new java.util.concurrent.CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    notificationService.createNotification(recipient.getId(), actor.getId(), NotificationType.REEL_LIKED, null, null, reel1.getId());
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = finishLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        List<Notification> notifs = notificationRepository.findAll();
        assertThat(notifs).hasSize(1);
        assertThat(notifs.get(0).getType()).isEqualTo(NotificationType.REEL_LIKED);
        assertThat(notifs.get(0).getReelId()).isEqualTo(reel1.getId());

        executor.shutdown();
    }
}

