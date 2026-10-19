package com.instagramclone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagramclone.admin.repository.ReportRepository;
import com.instagramclone.media.repository.MediaRepository;
import com.instagramclone.chat.repository.ConversationMemberRepository;
import com.instagramclone.chat.repository.ConversationRepository;
import com.instagramclone.chat.repository.MessageRepository;
import com.instagramclone.engagement.repository.CommentRepository;
import com.instagramclone.engagement.repository.LikeRepository;
import com.instagramclone.engagement.repository.SavedPostRepository;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.follow.repository.FollowRequestRepository;
import com.instagramclone.notification.repository.NotificationRepository;
import com.instagramclone.post.repository.PostMediaRepository;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.reel.repository.ReelRepository;
import com.instagramclone.security.JwtTokenProvider;
import com.instagramclone.story.repository.StoryRepository;
import com.instagramclone.story.repository.StoryViewRepository;
import com.instagramclone.user.repository.ProfileRepository;
import com.instagramclone.auth.repository.RefreshTokenRepository;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import com.instagramclone.user.repository.UserKnownDeviceRepository;
import com.instagramclone.user.repository.EmailChangeRequestRepository;
import com.instagramclone.post.repository.PostMediaTagRepository;
import com.instagramclone.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected MediaRepository mediaRepository;

    @Autowired
    protected ReportRepository reportRepository;

    @Autowired
    protected StoryViewRepository storyViewRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryMentionRepository storyMentionRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryLikeRepository storyLikeRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryHighlightItemRepository storyHighlightItemRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryHighlightRepository storyHighlightRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryPollVoteRepository storyPollVoteRepository;

    @Autowired
    protected com.instagramclone.story.repository.StoryPollRepository storyPollRepository;

    @Autowired
    protected com.instagramclone.user.repository.CloseFriendRepository closeFriendRepository;

    @Autowired
    protected StoryRepository storyRepository;

    @Autowired
    protected ReelRepository reelRepository;

    @Autowired
    protected com.instagramclone.reel.repository.ReelLikeRepository reelLikeRepository;

    @Autowired
    protected com.instagramclone.reel.repository.ReelCommentRepository reelCommentRepository;

    @Autowired
    protected com.instagramclone.reel.repository.ReelViewRepository reelViewRepository;

    @Autowired
    protected MessageRepository messageRepository;

    @Autowired
    protected com.instagramclone.chat.repository.MessageReactionRepository messageReactionRepository;

    @Autowired
    protected ConversationMemberRepository conversationMemberRepository;

    @Autowired
    protected ConversationRepository conversationRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected LikeRepository likeRepository;

    @Autowired
    protected SavedPostRepository savedPostRepository;

    @Autowired
    protected PostMediaRepository postMediaRepository;

    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected FollowRepository followRepository;

    @Autowired
    protected UserBlockRepository userBlockRepository;

    @Autowired
    protected UserSettingsRepository userSettingsRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected com.instagramclone.mail.repository.EmailDeliveryLogRepository emailDeliveryLogRepository;

    @Autowired
    protected com.instagramclone.auth.repository.EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    protected com.instagramclone.auth.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected ProfileRepository profileRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PostMediaTagRepository postMediaTagRepository;

    @Autowired
    protected UserKnownDeviceRepository userKnownDeviceRepository;

    @Autowired
    protected EmailChangeRequestRepository emailChangeRequestRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected FollowRequestRepository followRequestRepository;

    @Autowired(required = false)
    protected com.instagramclone.ratelimit.RedisRateLimiterService redisRateLimiterService;

    @Autowired(required = false)
    protected org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    protected com.instagramclone.security.otp.IEmailOtpService emailOtpService;

    @Autowired
    protected com.instagramclone.auth.repository.SignupVerificationTicketRepository signupVerificationTicketRepository;

    @Autowired
    protected com.instagramclone.search.repository.SearchHistoryRepository searchHistoryRepository;

    @Autowired(required = false)
    protected com.instagramclone.mail.repository.DynamicEmailConfigRepository dynamicEmailConfigRepository;

    @Autowired(required = false)
    protected com.instagramclone.mail.service.interfaces.IDynamicEmailConfigService dynamicEmailConfigService;

    @Autowired(required = false)
    protected com.instagramclone.mail.repository.DynamicEmailTemplateRepository dynamicEmailTemplateRepository;

    @Autowired(required = false)
    protected com.instagramclone.mail.service.interfaces.IDynamicEmailTemplateService dynamicEmailTemplateService;

    protected void cleanDatabase() {
        if (dynamicEmailConfigRepository != null) {
            dynamicEmailConfigRepository.deleteAll();
        }
        if (dynamicEmailConfigService != null) {
            dynamicEmailConfigService.initDefaultConfigs();
        }
        if (dynamicEmailTemplateRepository != null) {
            dynamicEmailTemplateRepository.deleteAll();
        }
        if (dynamicEmailTemplateService != null) {
            dynamicEmailTemplateService.initDefaultTemplates();
        }
        if (signupVerificationTicketRepository != null) {
            signupVerificationTicketRepository.deleteAll();
        }
        searchHistoryRepository.deleteAll();
        postMediaTagRepository.deleteAll();
        mediaRepository.deleteAll();
        reportRepository.deleteAll();
        auditLogRepository.deleteAll();
        storyPollVoteRepository.deleteAll();
        storyPollRepository.deleteAll();
        storyHighlightItemRepository.deleteAll();
        storyHighlightRepository.deleteAll();
        storyLikeRepository.deleteAll();
        storyMentionRepository.deleteAll();
        storyViewRepository.deleteAll();
        storyRepository.deleteAll();
        closeFriendRepository.deleteAll();
        notificationRepository.deleteAll();
        reelLikeRepository.deleteAll();
        reelCommentRepository.deleteAll();
        reelViewRepository.deleteAll();
        reelRepository.deleteAll();
        messageReactionRepository.deleteAll();
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();
        commentRepository.deleteAll();
        likeRepository.deleteAll();
        savedPostRepository.deleteAll();
        postMediaRepository.deleteAll();
        postRepository.deleteAll();
        followRepository.deleteAll();
        followRequestRepository.deleteAll();
        userBlockRepository.deleteAll();
        userKnownDeviceRepository.deleteAll();
        emailChangeRequestRepository.deleteAll();
        emailDeliveryLogRepository.deleteAll();
        emailDeliveryLogRepository.flush();
        userSettingsRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        profileRepository.deleteAll();
        profileRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
        if (redisRateLimiterService != null) {
            redisRateLimiterService.clearLimits();
        }
        if (stringRedisTemplate != null) {
            try {
                java.util.Set<String> otpKeys = stringRedisTemplate.keys("otp:*");
                if (otpKeys != null && !otpKeys.isEmpty()) {
                    stringRedisTemplate.delete(otpKeys);
                }
            } catch (Exception ignored) {
            }
        }
        if (emailOtpService instanceof com.instagramclone.security.otp.EmailOtpServiceImpl impl) {
            impl.clearAllLocalState();
        }
    }

    protected String createValidSignupTicket(String email) {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        String rawToken = java.util.HexFormat.of().formatHex(randomBytes);
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String hash = java.util.HexFormat.of().formatHex(digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            com.instagramclone.auth.entity.SignupVerificationTicket ticket = com.instagramclone.auth.entity.SignupVerificationTicket.builder()
                    .email(email.trim().toLowerCase())
                    .tokenHash(hash)
                    .expiresAt(java.time.LocalDateTime.now().plusSeconds(600))
                    .build();
            signupVerificationTicketRepository.save(ticket);
            return rawToken;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
