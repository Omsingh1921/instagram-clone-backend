package com.instagramclone.common;

import com.instagramclone.admin.constants.AdminConstants;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.util.AdminUtils;
import com.instagramclone.audit.constants.AuditConstants;
import com.instagramclone.audit.util.AuditUtils;
import com.instagramclone.chat.constants.ChatConstants;
import com.instagramclone.chat.util.ChatUtils;
import com.instagramclone.engagement.constants.EngagementConstants;
import com.instagramclone.engagement.util.EngagementUtils;
import com.instagramclone.feed.constants.FeedConstants;
import com.instagramclone.feed.util.FeedUtils;
import com.instagramclone.follow.constants.FollowConstants;
import com.instagramclone.follow.util.FollowUtils;
import com.instagramclone.mail.constants.MailConstants;
import com.instagramclone.mail.util.MailUtils;
import com.instagramclone.media.constants.MediaConstants;
import com.instagramclone.media.util.MediaUtils;
import com.instagramclone.notification.constants.NotificationConstants;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.util.NotificationUtils;
import com.instagramclone.post.constants.PostConstants;
import com.instagramclone.post.util.PostUtils;
import com.instagramclone.ratelimit.constants.RateLimitConstants;
import com.instagramclone.ratelimit.util.RateLimitUtils;
import com.instagramclone.reel.constants.ReelConstants;
import com.instagramclone.reel.util.ReelUtils;
import com.instagramclone.security.constants.SecurityModuleConstants;
import com.instagramclone.security.util.SecurityModuleUtils;
import com.instagramclone.search.constants.SearchConstants;
import com.instagramclone.search.util.SearchUtils;
import com.instagramclone.story.constants.StoryConstants;
import com.instagramclone.story.util.StoryUtils;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.util.UserUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModularArchitectureTest {

    @Test
    @DisplayName("User Module - UserUtils & Constants verification")
    void testUserModule() {
        assertThat(UserConstants.MAX_ACCOUNTS_PER_EMAIL).isEqualTo(3);
        assertThat(UserUtils.maskEmail("peter.parker@example.com")).isEqualTo("p***r@example.com");
        assertThat(UserUtils.maskEmail("a@b.com")).isEqualTo("a@b.com");
        assertThat(UserUtils.maskEmail(null)).isEmpty();
        assertThat(UserUtils.normalizeUsername("  Peter_Parker  ")).isEqualTo("peter_parker");
        assertThat(UserUtils.normalizeEmail("  TEST@Domain.COM  ")).isEqualTo("test@domain.com");
        assertThat(UserUtils.resolveDisplayName("Peter", "spidey")).isEqualTo("Peter");
        assertThat(UserUtils.resolveDisplayName("  ", "spidey")).isEqualTo("spidey");
        assertThat(UserUtils.isValidAccountSlot(1)).isTrue();
        assertThat(UserUtils.isValidAccountSlot(3)).isTrue();
        assertThat(UserUtils.isValidAccountSlot(4)).isFalse();
        assertThat(UserUtils.isValidAccountSlot(0)).isFalse();
    }

    @Test
    @DisplayName("Post Module - PostUtils & Constants verification")
    void testPostModule() {
        assertThat(PostConstants.MAX_CAPTION_LENGTH).isEqualTo(2200);
        String caption = "Enjoying #sunset in #NYC with @peter_parker and @mj!";
        Set<String> tags = PostUtils.extractHashtags(caption);
        assertThat(tags).containsExactlyInAnyOrder("sunset", "nyc");

        Set<String> mentions = PostUtils.extractMentions(caption);
        assertThat(mentions).containsExactlyInAnyOrder("peter_parker", "mj");

        assertThat(PostUtils.truncateCaption("Hello World", 5)).isEqualTo("He...");
        assertThat(PostUtils.truncateCaption("Hello", 10)).isEqualTo("Hello");
        assertThat(PostUtils.isCarousel(2)).isTrue();
        assertThat(PostUtils.isCarousel(1)).isFalse();
    }

    @Test
    @DisplayName("Reel Module - ReelUtils & Constants verification")
    void testReelModule() {
        assertThat(ReelConstants.MAX_DURATION_SECONDS).isEqualTo(90);
        assertThat(ReelUtils.resolveAudioTitle(null, "peter")).isEqualTo("peter • Original Audio");
        assertThat(ReelUtils.resolveAudioTitle("Trending Beats", "peter")).isEqualTo("Trending Beats");
        assertThat(ReelUtils.isValidDuration(15)).isTrue();
        assertThat(ReelUtils.isValidDuration(2)).isFalse();
        assertThat(ReelUtils.isValidDuration(95)).isFalse();
        assertThat(ReelUtils.calculateCompletionRate(45, 90)).isEqualTo(50.0);
        assertThat(ReelUtils.calculateCompletionRate(120, 90)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Story Module - StoryUtils & Constants verification")
    void testStoryModule() {
        assertThat(StoryConstants.STORY_LIFETIME_HOURS).isEqualTo(24);
        Instant recent = Instant.now().minus(Duration.ofHours(2));
        Instant old = Instant.now().minus(Duration.ofHours(26));
        assertThat(StoryUtils.isStoryActive(recent)).isTrue();
        assertThat(StoryUtils.isStoryActive(old)).isFalse();
        assertThat(StoryUtils.calculateRemainingSeconds(recent)).isGreaterThan(0);
        assertThat(StoryUtils.calculateRemainingSeconds(old)).isEqualTo(0);
    }

    @Test
    @DisplayName("Chat Module - ChatUtils & Constants verification")
    void testChatModule() {
        assertThat(ChatConstants.MAX_MESSAGE_LENGTH).isEqualTo(2000);
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertThat(ChatUtils.getDirectConversationKey(u1, u2)).isEqualTo(u1 + ":" + u2);
        assertThat(ChatUtils.getDirectConversationKey(u2, u1)).isEqualTo(u1 + ":" + u2);
        assertThat(ChatUtils.isSelfConversation(u1, u1)).isTrue();
        assertThat(ChatUtils.isSelfConversation(u1, u2)).isFalse();
        assertThat(ChatUtils.truncatePreview("A long text message", 8)).isEqualTo("A lon...");
    }

    @Test
    @DisplayName("Engagement Module - EngagementUtils & Constants verification")
    void testEngagementModule() {
        assertThat(EngagementConstants.MAX_COMMENT_LENGTH).isEqualTo(1000);
        assertThat(EngagementUtils.sanitizeComment("   Nice post!   ")).isEqualTo("Nice post!");
        assertThat(EngagementUtils.isValidDepth(2)).isTrue();
        assertThat(EngagementUtils.isValidDepth(4)).isFalse();
        assertThat(EngagementUtils.calculateEngagementRate(100, 20, 1000)).isEqualTo(12.0);
        assertThat(EngagementUtils.calculateEngagementRate(50, 10, 0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Follow Module - FollowUtils & Constants verification")
    void testFollowModule() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        assertThat(FollowUtils.isSelfFollow(u1, u1)).isTrue();
        assertThat(FollowUtils.isSelfFollow(u1, u2)).isFalse();
        assertThat(FollowUtils.formatCount(850)).isEqualTo("850");
        assertThat(FollowUtils.formatCount(1500)).isEqualTo("1.5K");
        assertThat(FollowUtils.formatCount(2300000)).isEqualTo("2.3M");
    }

    @Test
    @DisplayName("Feed Module - FeedUtils & Constants verification")
    void testFeedModule() {
        assertThat(FeedConstants.DEFAULT_FEED_SIZE).isEqualTo(15);
        double recentScore = FeedUtils.calculatePostScore(Instant.now(), 50, 10);
        double oldScore = FeedUtils.calculatePostScore(Instant.now().minus(Duration.ofDays(2)), 50, 10);
        assertThat(recentScore).isGreaterThan(oldScore);
    }

    @Test
    @DisplayName("Notification Module - NotificationUtils & Constants verification")
    void testNotificationModule() {
        assertThat(NotificationConstants.NOTIFICATION_EXPIRY_DAYS).isEqualTo(30);
        assertThat(NotificationUtils.buildNotificationMessage("peter", NotificationType.POST_LIKED))
                .isEqualTo("peter liked your post.");
        assertThat(NotificationUtils.buildNotificationMessage("mj", NotificationType.POST_COMMENTED))
                .isEqualTo("mj commented on your post.");
        UUID target = UUID.randomUUID();
        assertThat(NotificationUtils.generateGroupingKey(NotificationType.POST_LIKED, target))
                .isEqualTo("notif:post_liked:" + target);
    }

    @Test
    @DisplayName("Search Module - SearchUtils & Constants verification")
    void testSearchModule() {
        assertThat(SearchConstants.MAX_QUERY_LENGTH).isEqualTo(100);
        assertThat(SearchUtils.cleanQuery("   select * from users%_   ")).isEqualTo("select * from users");
        assertThat(SearchUtils.isHashtagSearch("#travel")).isTrue();
        assertThat(SearchUtils.isHashtagSearch("travel")).isFalse();
        assertThat(SearchUtils.normalizeTag("#Summer2026")).isEqualTo("summer2026");
    }

    @Test
    @DisplayName("Admin Module - AdminUtils & Constants verification")
    void testAdminModule() {
        assertThat(AdminConstants.MAX_REPORT_REASON_LENGTH).isEqualTo(1000);
        assertThat(AdminUtils.isTerminalReportStatus(ReportStatus.RESOLVED)).isTrue();
        assertThat(AdminUtils.isTerminalReportStatus(ReportStatus.DISMISSED)).isTrue();
        assertThat(AdminUtils.isTerminalReportStatus(ReportStatus.OPEN)).isFalse();
        assertThat(AdminUtils.formatResolutionNote("mod_lead", "User banned for spam"))
                .isEqualTo("[mod_lead] User banned for spam");
    }

    @Test
    @DisplayName("Mail Module - MailUtils & Constants verification")
    void testMailModule() {
        assertThat(MailConstants.DEFAULT_FROM_NAME).isEqualTo("Instagram Clone");
        assertThat(MailUtils.maskRecipient("john.doe@example.com")).isEqualTo("j***e@example.com");
        assertThat(MailUtils.cleanEmail("  JOHN.DOE@GMAIL.COM  ")).isEqualTo("john.doe@gmail.com");
    }

    @Test
    @DisplayName("Audit Module - AuditUtils & Constants verification")
    void testAuditModule() {
        assertThat(AuditConstants.MAX_ACTION_LENGTH).isEqualTo(100);
        assertThat(AuditUtils.extractClientIp("192.168.1.1, 10.0.0.1")).isEqualTo("192.168.1.1");
        assertThat(AuditUtils.extractClientIp(null)).isEqualTo("0.0.0.0");
        assertThat(AuditUtils.truncateResource("USER:12345")).isEqualTo("USER:12345");
    }

    @Test
    @DisplayName("Media Module - MediaUtils & Constants verification")
    void testMediaModule() {
        assertThat(MediaConstants.MAX_IMAGE_SIZE_BYTES).isEqualTo(10L * 1024 * 1024);
        assertThat(MediaUtils.isSupportedImage("image/jpeg")).isTrue();
        assertThat(MediaUtils.isSupportedImage("image/png")).isTrue();
        assertThat(MediaUtils.isSupportedImage("application/pdf")).isFalse();
        assertThat(MediaUtils.isSupportedVideo("video/mp4")).isTrue();
        assertThat(MediaUtils.isSupportedVideo("image/png")).isFalse();
        assertThat(MediaUtils.formatFileSize(500)).isEqualTo("500 B");
        assertThat(MediaUtils.formatFileSize(1500)).isEqualTo("1.5 KB");
        assertThat(MediaUtils.formatFileSize(2500000)).isEqualTo("2.4 MB");
    }

    @Test
    @DisplayName("Security Module - SecurityModuleUtils & Constants verification")
    void testSecurityModule() {
        assertThat(SecurityModuleConstants.AUTH_HEADER).isEqualTo("Authorization");
        assertThat(SecurityModuleUtils.extractBearerToken("Bearer mySecretJwtToken123"))
                .isEqualTo("mySecretJwtToken123");
        assertThat(SecurityModuleUtils.extractBearerToken("Basic dXNlcjpwYXNz")).isNull();
        assertThat(SecurityModuleUtils.extractBearerToken(null)).isNull();
    }

    @Test
    @DisplayName("RateLimit Module - RateLimitUtils & Constants verification")
    void testRateLimitModule() {
        assertThat(RateLimitConstants.DEFAULT_WINDOW_SECONDS).isEqualTo(60);
        assertThat(RateLimitUtils.buildKey("login", "peter")).isEqualTo("ratelimit:login:peter");
        assertThat(RateLimitUtils.calculateRetryAfter(60, 20)).isEqualTo(40);
        assertThat(RateLimitUtils.calculateRetryAfter(60, 70)).isEqualTo(1);
    }
}
