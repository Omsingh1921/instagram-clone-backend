package com.instagramclone.mail;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.common.constants.KafkaConstants;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.entity.EmailDeliveryLog;
import com.instagramclone.mail.event.EmailEvent;
import com.instagramclone.mail.event.EmailEventListener;
import com.instagramclone.mail.enums.EmailCategory;
import com.instagramclone.mail.enums.EmailDeliveryStatus;
import com.instagramclone.mail.enums.EmailType;
import com.instagramclone.mail.provider.IEmailProvider;
import com.instagramclone.mail.provider.SmtpEmailProvider;
import com.instagramclone.mail.repository.EmailDeliveryLogRepository;
import com.instagramclone.mail.service.impl.EmailDispatcher;
import com.instagramclone.mail.service.interfaces.IEmailPreferenceService;
import com.instagramclone.mail.service.interfaces.IMailService;
import com.instagramclone.mail.template.EmailTemplateRenderer;
import com.instagramclone.mail.template.RenderedEmail;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EmailOtpDeliveryTest extends BaseIntegrationTest {

    @Autowired
    private EmailTemplateRenderer templateRenderer;

    @Autowired
    private EmailDispatcher emailDispatcher;

    @Autowired
    private IMailService mailService;

    @Autowired
    private IEmailPreferenceService emailPreferenceService;

    @Autowired
    private EmailDeliveryLogRepository emailDeliveryLogRepository;

    @Autowired
    private EmailEventListener emailEventListener;

    @MockBean
    private IEmailProvider emailProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        testUser = User.builder()
                .username("jane_doe")
                .email("jane.doe@example.com")
                .passwordHash(passwordEncoder.encode("SecurePassword123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        testUser = userRepository.save(testUser);
    }

    // --- 1. Template Rendering & XSS Tests ---

    @Test
    @DisplayName("Template - Renders 6-digit OTP code preserving leading zeroes and prominent styling")
    void templateRenderer_RendersSixDigitCodeWithLeadingZeroes() {
        String leadingZeroOtp = "007892";
        Map<String, Object> vars = Map.of(
                "username", "jane_doe",
                "otpCode", leadingZeroOtp,
                "validityMinutes", 5
        );

        RenderedEmail rendered = templateRenderer.render(EmailType.EMAIL_OTP, vars, null);

        assertThat(rendered.subject()).isEqualTo("007892 is your Instagram clone verification code");
        assertThat(rendered.htmlBody()).contains("007892");
        assertThat(rendered.htmlBody()).contains("jane_doe");
        assertThat(rendered.htmlBody()).contains("5 minutes");
        assertThat(rendered.htmlBody()).contains("Security Notice");
        assertThat(rendered.htmlBody()).contains("background: #262626"); // Security dark header
        assertThat(rendered.htmlBody()).doesNotContain("Unsubscribe from this category"); // No unsub for security
        assertThat(rendered.textBody()).contains("007892");
        assertThat(rendered.textBody()).contains("5 minutes");
    }

    @Test
    @DisplayName("Template - Sanitizes and escapes malicious inputs (Anti-XSS protection)")
    void templateRenderer_EscapesMaliciousInput() {
        String xssPayload = "<script>alert('xss')</script>";
        Map<String, Object> vars = Map.of(
                "username", xssPayload,
                "otpCode", "<img src=x onerror=alert(1)>"
        );

        RenderedEmail rendered = templateRenderer.render(EmailType.EMAIL_OTP, vars, null);

        assertThat(rendered.htmlBody()).doesNotContain("<script>");
        assertThat(rendered.htmlBody()).contains("&lt;script&gt;");
        assertThat(rendered.htmlBody()).doesNotContain("<img src=x");
        assertThat(rendered.htmlBody()).contains("&lt;img src=x");
    }

    @Test
    @DisplayName("Template - Gracefully handles default/missing variables")
    void templateRenderer_HandlesMissingVariablesGracefully() {
        RenderedEmail rendered = templateRenderer.render(EmailType.EMAIL_OTP, Map.of(), null);

        assertThat(rendered.subject()).contains("is your Instagram clone verification code");
        assertThat(rendered.htmlBody()).contains("user");
        assertThat(rendered.htmlBody()).contains("5 minutes");
        assertThat(rendered.textBody()).isNotBlank();
    }

    // --- 2. Dispatcher Integration & Policy Enforcement ---

    @Test
    @DisplayName("Delivery - Delivers simulated when provider is offline/mocked with sanitized audit subject")
    void delivery_SimulatedModeWhenProviderUnavailable() {
        when(emailProvider.isAvailable()).thenReturn(false);

        EmailDeliveryLog log = mailService.sendOtpEmail(testUser.getEmail(), testUser.getUsername(), "123456");

        assertThat(log).isNotNull();
        assertThat(log.getStatus()).isEqualTo(EmailDeliveryStatus.DELIVERED_SIMULATED);
        assertThat(log.getEmailType()).isEqualTo(EmailType.EMAIL_OTP);
        assertThat(log.getCategory()).isEqualTo(EmailCategory.ACCOUNT);
        assertThat(log.getAttempts()).isEqualTo(1);
        assertThat(log.getRecipientEmail()).isEqualTo(testUser.getEmail());
        // Finding 1 Verification: audit entity subject MUST be generic and never contain the OTP
        assertThat(log.getSubject()).isEqualTo("Your Instagram clone verification code");
        assertThat(log.getSubject()).doesNotContain("123456");
        assertThat(log.getSentAt()).isNotNull();

        // Verify persisted in database
        EmailDeliveryLog persisted = emailDeliveryLogRepository.findById(log.getId()).orElseThrow();
        assertThat(persisted.getSubject()).isEqualTo("Your Instagram clone verification code");
        assertThat(persisted.getSubject()).doesNotContain("123456");
    }

    @Test
    @DisplayName("Delivery - Dispatches via active provider: Outbound gets OTP subject, audit log is sanitized")
    void delivery_SuccessfulDispatchViaProvider() {
        when(emailProvider.isAvailable()).thenReturn(true);
        doNothing().when(emailProvider).send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        EmailDeliveryLog log = mailService.sendOtpEmail("test@example.com", "testuser", "987654");

        assertThat(log).isNotNull();
        assertThat(log.getStatus()).isEqualTo(EmailDeliveryStatus.SENT_TO_PROVIDER);
        assertThat(log.getAttempts()).isEqualTo(1);
        assertThat(log.getSentAt()).isNotNull();

        // Finding 1 Verification: Persisted audit log subject MUST NOT contain "987654"
        assertThat(log.getSubject()).isEqualTo("Your Instagram clone verification code");
        assertThat(log.getSubject()).doesNotContain("987654");

        ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailProvider, times(1)).send(
                recipientCaptor.capture(),
                subjectCaptor.capture(),
                htmlCaptor.capture(),
                anyString(),
                anyString(),
                anyString()
        );

        // Verification: Outbound email transport still receives recipient, raw OTP subject, and OTP in body
        assertThat(recipientCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("987654 is your Instagram clone verification code");
        assertThat(htmlCaptor.getValue()).contains("987654");
    }

    @Test
    @DisplayName("Delivery - Always delivers mandatory OTP even if user opted out of all optional notifications")
    void delivery_MandatoryBypassesAllNotificationPreferences() {
        when(emailProvider.isAvailable()).thenReturn(false);

        // Opt out of all notifications
        emailPreferenceService.updatePreferences(testUser.getId(), UpdateEmailPreferencesRequest.builder()
                .emailSocialFollows(false)
                .emailSocialLikes(false)
                .emailSocialComments(false)
                .emailMessageReminders(false)
                .emailActivityDigest(false)
                .emailProductAnnouncements(false)
                .build());

        EmailDeliveryLog log = mailService.sendOtpEmail(testUser.getEmail(), testUser.getUsername(), "555111");

        assertThat(log).isNotNull();
        // Crucial: Must NOT be SKIPPED_PREFERENCE
        assertThat(log.getStatus()).isEqualTo(EmailDeliveryStatus.DELIVERED_SIMULATED);
        assertThat(log.getEmailType()).isEqualTo(EmailType.EMAIL_OTP);
        assertThat(log.getSubject()).doesNotContain("555111");
    }

    // --- 3. Failure Handling & Bounded Retries ---

    @Test
    @DisplayName("Failure Handling - Bounded retries and FAILED status recording with sanitized lastError")
    void failureHandling_RetriesAndMarksFailedOnProviderError() {
        when(emailProvider.isAvailable()).thenReturn(true);
        doThrow(new RuntimeException("SMTP Connection Timeout for recipient failure@example.com with code 404040: 504 Gateway Timeout"))
                .when(emailProvider).send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        EmailDeliveryLog log = mailService.sendOtpEmail("failure@example.com", "failure_user", "404040");

        assertThat(log).isNotNull();
        assertThat(log.getStatus()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(log.getAttempts()).isEqualTo(3); // Retried maxRetries (3) times
        assertThat(log.getLastError()).contains("SMTP Connection Timeout");

        // Finding 1 & 2 Verification: lastError must NOT leak plaintext OTP code
        assertThat(log.getLastError()).doesNotContain("404040");
        assertThat(log.getLastError()).contains("[REDACTED_OTP]");
        // lastError must NOT leak raw email address
        assertThat(log.getLastError()).doesNotContain("failure@example.com");
        assertThat(log.getLastError()).contains("f***e@example.com");

        // Verify provider was indeed called 3 times
        verify(emailProvider, times(3)).send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // --- 4. Event-Driven Integration ---

    @Test
    @DisplayName("Event Listener - Consumes EmailEvent with EMAIL_OTP type and executes dispatch")
    void eventListener_ProcessesEmailOtpEvent() {
        when(emailProvider.isAvailable()).thenReturn(false);

        EmailEvent event = EmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(KafkaConstants.EVENT_EMAIL_OTP_REQUESTED)
                .emailType(EmailType.EMAIL_OTP)
                .email("event@example.com")
                .username("event_user")
                .rawToken("332211")
                .variables(Map.of("username", "event_user", "otpCode", "332211", "validityMinutes", 5))
                .timestamp(LocalDateTime.now())
                .build();

        emailEventListener.processWithDeduplication(event, "TEST_SUITE");

        // Verify delivery was recorded with sanitized subject
        var logs = emailDeliveryLogRepository.findAll();
        var otpLog = logs.stream()
                .filter(l -> "event@example.com".equals(l.getRecipientEmail()) && l.getEmailType() == EmailType.EMAIL_OTP)
                .findFirst();

        assertThat(otpLog).isPresent();
        assertThat(otpLog.get().getSubject()).isEqualTo("Your Instagram clone verification code");
        assertThat(otpLog.get().getSubject()).doesNotContain("332211");
    }

    // --- 5. Security & Remediation Verification Tests ---

    @Test
    @DisplayName("Security Finding 1 - Persisted audit log strictly never contains plaintext OTP across all fields")
    void persistedAuditLog_NeverContainsPlaintextOtp() {
        when(emailProvider.isAvailable()).thenReturn(false);

        String secretOtp = "003456";
        EmailDeliveryLog log = mailService.sendOtpEmail("audit_test@example.com", "audit_user", secretOtp);

        EmailDeliveryLog persisted = emailDeliveryLogRepository.findById(log.getId()).orElseThrow();

        assertThat(persisted.getSubject()).isEqualTo("Your Instagram clone verification code");
        assertThat(persisted.getSubject()).doesNotContain(secretOtp);
        if (persisted.getLastError() != null) {
            assertThat(persisted.getLastError()).doesNotContain(secretOtp);
        }
    }

    @Test
    @DisplayName("Security Finding 2 - Loggers suppress plaintext OTP and raw email address in output")
    void logging_SuppressesRawEmailAndOtpInLogs() {
        when(emailProvider.isAvailable()).thenReturn(false);

        Logger dispatcherLogger = (Logger) LoggerFactory.getLogger(EmailDispatcher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        dispatcherLogger.addAppender(appender);

        String secretEmail = "secret_agent@example.com";
        String secretOtp = "042891";

        try {
            mailService.sendOtpEmail(secretEmail, "agent", secretOtp);

            assertThat(appender.list).isNotEmpty();
            for (ILoggingEvent loggingEvent : appender.list) {
                String formattedMessage = loggingEvent.getFormattedMessage();
                // Crucial: OTP must never appear in application logs
                assertThat(formattedMessage).doesNotContain(secretOtp);
                // Crucial: Raw email must never appear in application logs
                assertThat(formattedMessage).doesNotContain(secretEmail);
                // Masked email must appear
                assertThat(formattedMessage).contains("s***t@example.com");
            }
        } finally {
            dispatcherLogger.detachAppender(appender);
        }
    }
}
