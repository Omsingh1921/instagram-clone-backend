package com.instagramclone.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.mail.dto.EmailCommand;
import com.instagramclone.mail.dto.request.UnsubscribeRequest;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventListener;
import com.instagramclone.mail.enums.EmailCategory;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailPriority;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.service.impl.EmailDispatcher;
import com.instagramclone.mail.service.interfaces.IEmailPreferenceService;
import com.instagramclone.mail.template.EmailTemplateRenderer;
import com.instagramclone.mail.template.RenderedEmail;
import com.instagramclone.auth.dto.request.LoginRequest;
import com.instagramclone.auth.dto.request.RegisterRequest;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.utility.RedisUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MultiCategoryEmailTest extends BaseIntegrationTest {

    @Autowired
    private EmailDispatcher emailDispatcher;

    @Autowired
    private IEmailPreferenceService emailPreferenceService;

    @Autowired
    private EmailTemplateRenderer emailTemplateRenderer;

    @Autowired
    private EmailEventListener emailEventListener;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtility redisUtility;

    private User primaryUser;
    private String primaryUserToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        primaryUser = User.builder()
                .username("bruce_wayne")
                .email("bruce@wayne-enterprises.com")
                .passwordHash(passwordEncoder.encode("DarkKnight2026!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        primaryUser = userRepository.save(primaryUser);

        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("bruce_wayne")
                .password("DarkKnight2026!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        primaryUserToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("EmailTemplateRenderer - Properly sanitizes and escapes malicious script tags (Anti-XSS)")
    void templateRenderer_EscapesMaliciousInput() {
        String xssPayload = "<script>alert('pwned')</script>";
        Map<String, Object> vars = Map.of(
                "actorUsername", xssPayload,
                "commentText", "<img src=x onerror=alert('steal_cookie')>"
        );

        RenderedEmail rendered = emailTemplateRenderer.render(EmailType.POST_COMMENTED, vars, "http://example.com/unsub");

        assertThat(rendered.htmlBody()).doesNotContain("<script>");
        assertThat(rendered.htmlBody()).contains("&lt;script&gt;");
        assertThat(rendered.htmlBody()).doesNotContain("<img src=x");
        assertThat(rendered.htmlBody()).contains("&lt;img src=x");
    }

    @Test
    @DisplayName("EmailDispatcher - Delivers mandatory security emails regardless of preference settings")
    void dispatcher_MandatoryEmailsBypassPreferences() {
        // Explicitly disable all optional preferences
        emailPreferenceService.updatePreferences(primaryUser.getId(), UpdateEmailPreferencesRequest.builder()
                .emailSocialFollows(false)
                .emailSocialLikes(false)
                .emailSocialComments(false)
                .emailMessageReminders(false)
                .emailActivityDigest(false)
                .emailProductAnnouncements(false)
                .build());

        EmailCommand securityCmd = EmailCommand.builder()
                .emailType(EmailType.PASSWORD_CHANGED)
                .recipientEmail(primaryUser.getEmail())
                .recipientUserId(primaryUser.getId())
                .priority(EmailPriority.HIGH)
                .variables(Map.of("username", primaryUser.getUsername()))
                .build();

        EmailDeliveryLog log = emailDispatcher.dispatch(securityCmd);

        assertThat(log).isNotNull();
        assertThat(log.getStatus()).isIn(EmailDeliveryStatus.DELIVERED_SIMULATED, EmailDeliveryStatus.SENT_TO_PROVIDER);
        assertThat(log.getEmailType()).isEqualTo(EmailType.PASSWORD_CHANGED);
        assertThat(log.getCategory()).isEqualTo(EmailCategory.ACCOUNT);
    }

    @Test
    @DisplayName("EmailDispatcher - Honors user preferences and skips optional social emails when opted-out")
    void dispatcher_HonorsOptOutPreferences() {
        // Opt out of like notifications
        emailPreferenceService.updatePreferences(primaryUser.getId(), UpdateEmailPreferencesRequest.builder()
                .emailSocialLikes(false)
                .build());

        EmailCommand likeCmd = EmailCommand.builder()
                .emailType(EmailType.POST_LIKED)
                .recipientEmail(primaryUser.getEmail())
                .recipientUserId(primaryUser.getId())
                .priority(EmailPriority.LOW)
                .variables(Map.of("actorUsername", "clark_kent"))
                .build();

        EmailDeliveryLog log = emailDispatcher.dispatch(likeCmd);

        assertThat(log).isNotNull();
        assertThat(log.getStatus()).isEqualTo(EmailDeliveryStatus.SKIPPED_PREFERENCE);
        assertThat(log.getAttempts()).isZero();
    }

    @Test
    @DisplayName("EmailDispatcher - Debounces high-frequency social events to prevent inbox spam")
    void dispatcher_DebouncesDuplicateSocialEvents() {
        String actor = "barry_allen";
        UUID postId = UUID.randomUUID();

        EmailCommand firstLike = EmailCommand.builder()
                .emailType(EmailType.POST_LIKED)
                .recipientEmail(primaryUser.getEmail())
                .recipientUserId(primaryUser.getId())
                .priority(EmailPriority.LOW)
                .variables(Map.of("actorUsername", actor, "postId", postId))
                .build();

        EmailCommand secondLike = EmailCommand.builder()
                .emailType(EmailType.POST_LIKED)
                .recipientEmail(primaryUser.getEmail())
                .recipientUserId(primaryUser.getId())
                .priority(EmailPriority.LOW)
                .variables(Map.of("actorUsername", actor, "postId", postId))
                .build();

        EmailDeliveryLog log1 = emailDispatcher.dispatch(firstLike);
        EmailDeliveryLog log2 = emailDispatcher.dispatch(secondLike);

        assertThat(log1.getStatus()).isIn(EmailDeliveryStatus.DELIVERED_SIMULATED, EmailDeliveryStatus.SENT_TO_PROVIDER);
        assertThat(log2.getStatus()).isEqualTo(EmailDeliveryStatus.SKIPPED_THROTTLED);
    }

    @Test
    @DisplayName("IEmailPreferenceService - Generates valid HMAC unsubscribe URL and processes unauthenticated unsubscribe")
    void unsubscribeFlow_ValidTokenSucceeds() throws Exception {
        String unsubUrl = emailPreferenceService.generateUnsubscribeUrl(primaryUser.getId(), EmailType.POST_LIKED);
        assertThat(unsubUrl).isNotNull();
        assertThat(unsubUrl).contains("userId=" + primaryUser.getId());
        assertThat(unsubUrl).contains("emailType=POST_LIKED");

        // Parse query params
        String query = unsubUrl.substring(unsubUrl.indexOf('?') + 1);
        String[] pairs = query.split("&");
        long expires = 0;
        String signature = null;

        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if ("expires".equals(kv[0])) expires = Long.parseLong(kv[1]);
            if ("signature".equals(kv[0])) signature = kv[1];
        }

        UnsubscribeRequest unsubReq = UnsubscribeRequest.builder()
                .userId(primaryUser.getId())
                .emailType(EmailType.POST_LIKED)
                .expires(expires)
                .signature(signature)
                .build();

        mockMvc.perform(post("/api/v1/auth/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsubReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully unsubscribed from POST_LIKED notifications"));

        // Verify database updated
        assertThat(emailPreferenceService.isEmailAllowed(primaryUser.getId(), EmailType.POST_LIKED)).isFalse();
    }

    @Test
    @DisplayName("IEmailPreferenceService - Rejects unsubscribe requests with invalid or tampered signature")
    void unsubscribeFlow_TamperedSignatureRejected() throws Exception {
        UnsubscribeRequest invalidReq = UnsubscribeRequest.builder()
                .userId(primaryUser.getId())
                .emailType(EmailType.POST_LIKED)
                .expires(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .signature("invalid-forged-hmac-signature-1234567890abcdef")
                .build();

        mockMvc.perform(post("/api/v1/auth/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid unsubscribe signature"));
    }

    @Test
    @DisplayName("IEmailPreferenceService - Rejects unsubscribe requests for mandatory security emails")
    void unsubscribeFlow_MandatoryEmailRejected() throws Exception {
        UnsubscribeRequest mandatoryReq = UnsubscribeRequest.builder()
                .userId(primaryUser.getId())
                .emailType(EmailType.PASSWORD_RESET)
                .expires(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .signature("any-signature")
                .build();

        mockMvc.perform(post("/api/v1/auth/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mandatoryReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot unsubscribe from mandatory security or account notifications"));
    }

    @Test
    @DisplayName("User Email Preferences API - Authenticated user can retrieve and update preferences")
    void emailPreferencesApi_GetAndPutWorkForAuthenticatedUser() throws Exception {
        // 1. GET preferences
        mockMvc.perform(get("/api/v1/users/me/email-preferences")
                        .header("Authorization", "Bearer " + primaryUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailSocialFollows").value(true))
                .andExpect(jsonPath("$.data.emailSocialLikes").value(true));

        // 2. PUT preferences
        UpdateEmailPreferencesRequest updateReq = UpdateEmailPreferencesRequest.builder()
                .emailSocialFollows(false)
                .emailProductAnnouncements(true)
                .digestFrequency("DAILY")
                .build();

        mockMvc.perform(put("/api/v1/users/me/email-preferences")
                        .header("Authorization", "Bearer " + primaryUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailSocialFollows").value(false))
                .andExpect(jsonPath("$.data.emailProductAnnouncements").value(true))
                .andExpect(jsonPath("$.data.digestFrequency").value("DAILY"));

        // Verify unauthenticated attempt returns 401
        mockMvc.perform(get("/api/v1/users/me/email-preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Business Trigger - Successful login dispatches NEW_LOGIN security email")
    void loginTrigger_DispatchesSecurityEmail() throws Exception {
        emailDeliveryLogRepository.deleteAll();

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("bruce_wayne")
                .password("DarkKnight2026!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());

        List<EmailDeliveryLog> logs = java.util.Collections.emptyList();
        for (int i = 0; i < 20; i++) {
            logs = emailDeliveryLogRepository.findByUserIdAndEmailTypeOrderByCreatedAtDesc(
                    primaryUser.getId(), EmailType.NEW_LOGIN
            );
            if (!logs.isEmpty()) break;
            Thread.sleep(100);
        }

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getEmailType()).isEqualTo(EmailType.NEW_LOGIN);
        assertThat(logs.get(0).getCategory()).isEqualTo(EmailCategory.SECURITY);
    }

    @Test
    @DisplayName("Business Trigger - Account privacy toggle dispatches SECURITY_SETTINGS_CHANGED email")
    void privacyToggle_DispatchesSecuritySettingsChangedEmail() throws Exception {
        emailDeliveryLogRepository.deleteAll();

        UpdateSettingsRequest req = new UpdateSettingsRequest();
        req.setPrivateAccount(true);

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + primaryUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<EmailDeliveryLog> logs = java.util.Collections.emptyList();
        for (int i = 0; i < 20; i++) {
            logs = emailDeliveryLogRepository.findByUserIdAndEmailTypeOrderByCreatedAtDesc(
                    primaryUser.getId(), EmailType.SECURITY_SETTINGS_CHANGED
            );
            if (!logs.isEmpty()) break;
            Thread.sleep(100);
        }

        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getEmailType()).isEqualTo(EmailType.SECURITY_SETTINGS_CHANGED);
        assertThat(logs.get(0).getSubject()).contains("Account settings updated");
    }

    @Test
    @DisplayName("Kafka & Redis Idempotency - Redelivered events are deduplicated and executed only once")
    void idempotency_PreventsRedeliveryDuplication() {
        emailDeliveryLogRepository.deleteAll();
        String eventId = UUID.randomUUID().toString();
        EmailEvent event = EmailEvent.builder()
                .eventId(eventId)
                .emailType(EmailType.PRODUCT_ANNOUNCEMENT)
                .email("diana@themyscira.com")
                .userId(primaryUser.getId())
                .variables(Map.of("title", "New Reels Feature", "announcement", "Discover creative videos!"))
                .timestamp(LocalDateTime.now())
                .build();

        long countBefore = emailDeliveryLogRepository.findByUserIdAndEmailTypeOrderByCreatedAtDesc(
                primaryUser.getId(), EmailType.PRODUCT_ANNOUNCEMENT
        ).size();

        // 1st dispatch
        emailEventListener.processWithDeduplication(event, "SPRING_EVENT");
        long countAfterFirst = emailDeliveryLogRepository.findByUserIdAndEmailTypeOrderByCreatedAtDesc(
                primaryUser.getId(), EmailType.PRODUCT_ANNOUNCEMENT
        ).size();
        assertThat(countAfterFirst).isEqualTo(countBefore + 1);

        // 2nd duplicate dispatch with identical eventId
        emailEventListener.processWithDeduplication(event, "KAFKA");
        long countAfterSecond = emailDeliveryLogRepository.findByUserIdAndEmailTypeOrderByCreatedAtDesc(
                primaryUser.getId(), EmailType.PRODUCT_ANNOUNCEMENT
        ).size();

        // Count should not have incremented because Redis idempotency dropped the duplicate
        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }
}
