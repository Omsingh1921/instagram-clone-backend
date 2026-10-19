package com.instagramclone.admin;

import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.admin.constants.AdminConstants;
import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.dto.request.UpdateUserRoleRequest;
import com.instagramclone.admin.dto.request.UpdateUserStatusRequest;
import com.instagramclone.admin.entity.Report;
import com.instagramclone.admin.enums.ModerationAction;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.post.entity.Post;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Admin Module Integration Tests")
public class AdminIntegrationTest extends BaseIntegrationTest {

    private User admin;
    private User alice;
    private User bob;
    private String tokenAdmin;
    private String tokenAlice;
    private String tokenBob;
    private Post alicePost;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        admin = userRepository.save(User.builder()
                .username("superadmin")
                .email("admin@instagram.com")
                .passwordHash("hashed")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        alice = userRepository.save(User.builder()
                .username("alice")
                .email("alice@instagram.com")
                .passwordHash("hashed")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        bob = userRepository.save(User.builder()
                .username("bob")
                .email("bob@instagram.com")
                .passwordHash("hashed")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        tokenAdmin = jwtTokenProvider.generateTokenForUser(admin.getId(), admin.getUsername(), admin.getEmail());
        tokenAlice = jwtTokenProvider.generateTokenForUser(alice.getId(), alice.getUsername(), alice.getEmail());
        tokenBob = jwtTokenProvider.generateTokenForUser(bob.getId(), bob.getUsername(), bob.getEmail());

        alicePost = postRepository.save(Post.builder()
                .user(alice)
                .caption("Inappropriate content caption")
                .build());
    }

    @Test
    @DisplayName("Report Creation - Authenticated user can report content successfully")
    void createReport_Success() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("SPAM")
                .description("This post is promotional spam")
                .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_REPORT_SUBMITTED))
                .andExpect(jsonPath("$.data.targetType").value("POST"))
                .andExpect(jsonPath("$.data.targetId").value(alicePost.getId().toString()))
                .andExpect(jsonPath("$.data.reasonCode").value("SPAM"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.reporter.username").value("bob"));

        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Report Creation - Cannot report own content or self")
    void createReport_SelfReporting_BadRequest() throws Exception {
        // Alice reports her own post
        CreateReportRequest ownPostRequest = CreateReportRequest.builder()
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("INAPPROPRIATE")
                .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownPostRequest)))
                .andExpect(status().isBadRequest());

        // Alice reports her own user profile
        CreateReportRequest ownUserRequest = CreateReportRequest.builder()
                .targetType(ReportTargetType.USER)
                .targetId(alice.getId())
                .reasonCode("HARASSMENT")
                .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownUserRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Report Creation - Reporting non-existent content returns 404")
    void createReport_TargetNotFound_Returns404() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .targetType(ReportTargetType.POST)
                .targetId(UUID.randomUUID())
                .reasonCode("SPAM")
                .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security & RBAC - Non-admin user accessing admin endpoints receives 403 Forbidden")
    void nonAdmin_AccessingAdminEndpoints_Forbidden() throws Exception {
        // Bob tries to get reports
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // Bob tries to get dashboard stats
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_DASHBOARD_STATS_PATH)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // Bob tries to get users list
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_USERS_PATH)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // Bob tries to update user status
        UpdateUserStatusRequest statusRequest = UpdateUserStatusRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();
        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + alice.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Reports - Listing and single report detail retrieval")
    void adminReports_GetListAndDetail() throws Exception {
        Report report = reportRepository.save(Report.builder()
                .reporter(bob)
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("HATE_SPEECH")
                .description("Hate speech violation")
                .status(ReportStatus.OPEN)
                .build());

        // 1. Admin gets all reports
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_REPORTS_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(report.getId().toString()));

        // 2. Admin filters reports by status
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_REPORTS_PATH)
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        // 3. Admin filters by non-matching status
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_REPORTS_PATH)
                        .param("status", "RESOLVED")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // 4. Admin gets single report by ID
        mockMvc.perform(get(AdminConstants.BASE_PATH + "/admin/reports/" + report.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(report.getId().toString()))
                .andExpect(jsonPath("$.data.reasonCode").value("HATE_SPEECH"))
                .andExpect(jsonPath("$.data.targetType").value("POST"));
    }

    @Test
    @DisplayName("Admin Moderation - Resolving report with REMOVE_CONTENT takes down post")
    void adminResolveReport_RemoveContent_SoftDeletesPost() throws Exception {
        Report report = reportRepository.save(Report.builder()
                .reporter(bob)
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("COPYRIGHT")
                .description("Infringing image")
                .status(ReportStatus.OPEN)
                .build());

        ResolveReportRequest resolveRequest = ResolveReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .action(ModerationAction.REMOVE_CONTENT)
                .resolutionNotes("Verified copyright infringement. Content removed.")
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/reports/" + report.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.action").value("REMOVE_CONTENT"))
                .andExpect(jsonPath("$.data.reviewer.username").value("superadmin"))
                .andExpect(jsonPath("$.data.resolutionNotes").value("Verified copyright infringement. Content removed."));

        // Verify that the post was soft-deleted
        Post updatedPost = postRepository.findById(alicePost.getId()).orElseThrow();
        assertThat(updatedPost.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("Admin Moderation - Resolving report with SUSPEND_USER suspends author")
    void adminResolveReport_SuspendUser_SuspendsAuthor() throws Exception {
        Report report = reportRepository.save(Report.builder()
                .reporter(bob)
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("SEVERE_VIOLATION")
                .description("Malicious conduct")
                .status(ReportStatus.OPEN)
                .build());

        ResolveReportRequest resolveRequest = ResolveReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .action(ModerationAction.SUSPEND_USER)
                .resolutionNotes("Repeated malicious violations. User suspended.")
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/reports/" + report.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.action").value("SUSPEND_USER"));

        // Verify that Alice was suspended
        User updatedAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(updatedAlice.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(updatedAlice.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Admin User Management - Update user status and self-modification prevention")
    void adminUserManagement_UpdateStatus() throws Exception {
        // 1. Admin suspends Bob
        UpdateUserStatusRequest suspendRequest = UpdateUserStatusRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + bob.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendRequest)))
                .andExpect(status().isOk());

        User updatedBob = userRepository.findById(bob.getId()).orElseThrow();
        assertThat(updatedBob.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(updatedBob.getIsActive()).isFalse();

        // 2. Admin reactivates Bob
        UpdateUserStatusRequest activeRequest = UpdateUserStatusRequest.builder()
                .status(UserStatus.ACTIVE)
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + bob.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activeRequest)))
                .andExpect(status().isOk());

        User reactivatedBob = userRepository.findById(bob.getId()).orElseThrow();
        assertThat(reactivatedBob.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reactivatedBob.getIsActive()).isTrue();

        // 3. Admin self-status modification is blocked
        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + admin.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin User Management - Update user role and self-demotion prevention")
    void adminUserManagement_UpdateRole() throws Exception {
        // 1. Promote Bob to Moderator
        UpdateUserRoleRequest roleRequest = UpdateUserRoleRequest.builder()
                .role(Role.ROLE_MODERATOR)
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + bob.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_USER_ROLE_UPDATED));

        User updatedBob = userRepository.findById(bob.getId()).orElseThrow();
        assertThat(updatedBob.getRole()).isEqualTo(Role.ROLE_MODERATOR);

        // 2. Admin cannot change their own role
        UpdateUserRoleRequest selfRole = UpdateUserRoleRequest.builder()
                .role(Role.ROLE_USER)
                .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + "/admin/users/" + admin.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfRole)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin User Listing - Filter and search users")
    void adminUsers_SearchAndFilter() throws Exception {
        // Search query "alice"
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_USERS_PATH)
                        .param("query", "alice")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("alice"))
                .andExpect(jsonPath("$.data.content[0].postsCount").value(1));

        // Filter by status ACTIVE
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_USERS_PATH)
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("Admin Dashboard - Aggregate statistics")
    void adminDashboardStats_Success() throws Exception {
        // Create an open report
        reportRepository.save(Report.builder()
                .reporter(bob)
                .targetType(ReportTargetType.POST)
                .targetId(alicePost.getId())
                .reasonCode("SPAM")
                .status(ReportStatus.OPEN)
                .build());

        // Fetch dashboard metrics
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_DASHBOARD_STATS_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.activeUsers").value(3))
                .andExpect(jsonPath("$.data.totalPosts").value(1))
                .andExpect(jsonPath("$.data.totalReports").value(1))
                .andExpect(jsonPath("$.data.openReports").value(1));

        // Fetch using stats alias endpoint
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_STATS_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(3));
    }

    @Test
    @DisplayName("Admin Email Config - Admin can retrieve all category email configurations")
    void adminEmailConfigs_GetAllAndByCategory() throws Exception {
        // 1. Get all dynamic email configurations
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_EMAIL_CONFIGS_RETRIEVED))
                .andExpect(jsonPath("$.data.length()").value(5));

        // 2. Get specific category configuration (SECURITY)
        mockMvc.perform(get(AdminConstants.BASE_PATH + "/admin/email-configs/SECURITY")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("SECURITY"))
                .andExpect(jsonPath("$.data.purposeName").value("Security Alerts & Protection"));
    }

    @Test
    @DisplayName("Admin Email Config - Admin dynamically updates email for a specific work/purpose")
    void adminEmailConfigs_UpdateConfig_DynamicallyUpdatesSender() throws Exception {
        com.instagramclone.mail.dto.request.DynamicEmailConfigRequest updateRequest =
                com.instagramclone.mail.dto.request.DynamicEmailConfigRequest.builder()
                        .category(com.instagramclone.mail.enums.EmailCategory.SECURITY)
                        .fromEmail("cyber-security@instagram.com")
                        .senderName("Instagram Cyber Defense")
                        .purposeName("High Security Alerts")
                        .description("Dispatched for suspicious logins and 2FA alerts")
                        .isEnabled(true)
                        .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromEmail").value("cyber-security@instagram.com"))
                .andExpect(jsonPath("$.data.senderName").value("Instagram Cyber Defense"));

        // Verify that the dynamic sender is immediately resolved at runtime
        com.instagramclone.mail.config.MailProperties.SenderIdentity resolvedSender =
                dynamicEmailConfigService.resolveSender(com.instagramclone.mail.enums.EmailCategory.SECURITY);

        assertThat(resolvedSender.getFrom()).isEqualTo("cyber-security@instagram.com");
        assertThat(resolvedSender.getName()).isEqualTo("Instagram Cyber Defense");
    }

    @Test
    @DisplayName("Admin Email Config - Admin sends test email for verified routing")
    void adminEmailConfigs_SendTestEmail_Success() throws Exception {
        com.instagramclone.mail.dto.request.TestEmailConfigRequest testRequest =
                com.instagramclone.mail.dto.request.TestEmailConfigRequest.builder()
                        .category(com.instagramclone.mail.enums.EmailCategory.ACCOUNT)
                        .recipientEmail("admin-test@example.com")
                        .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_CONFIG_TEST_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_TEST_EMAIL_SENT));
    }

    @Test
    @DisplayName("Admin Email Config - Non-admin user accessing email configs receives 403 Forbidden")
    void adminEmailConfigs_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        com.instagramclone.mail.dto.request.DynamicEmailConfigRequest updateRequest =
                com.instagramclone.mail.dto.request.DynamicEmailConfigRequest.builder()
                        .category(com.instagramclone.mail.enums.EmailCategory.SECURITY)
                        .fromEmail("hacker@malicious.com")
                        .senderName("Fake Admin")
                        .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Email Templates - Admin can retrieve all templates and specific template by type")
    void adminEmailTemplates_GetAllAndByType() throws Exception {
        // 1. Get all templates
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_EMAIL_TEMPLATES_RETRIEVED))
                .andExpect(jsonPath("$.data.length()").isNotEmpty());

        // 2. Get specific template by type (WELCOME)
        mockMvc.perform(get(AdminConstants.BASE_PATH + "/admin/email-templates/WELCOME")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailType").value("WELCOME"))
                .andExpect(jsonPath("$.data.templateName").value("Welcome Email"))
                .andExpect(jsonPath("$.data.availablePlaceholders").value("{{username}}, {{exploreUrl}}"));
    }

    @Test
    @DisplayName("Admin Email Templates - Admin can update template, preview rendered output, and verify dynamic rendering")
    void adminEmailTemplates_UpdateAndPreview() throws Exception {
        // 1. Update WELCOME template with custom message
        com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest updateRequest =
                com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest.builder()
                        .emailType(com.instagramclone.mail.enums.EmailType.WELCOME)
                        .templateName("VIP Welcome Email")
                        .subjectTemplate("Welcome to our exclusive community, {{username}}!")
                        .bodyHtmlTemplate("<p>Hello <strong>{{username}}</strong>, welcome to our VIP social network!</p>")
                        .bodyTextTemplate("Hello {{username}}, welcome to our VIP network!")
                        .isActive(true)
                        .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjectTemplate").value("Welcome to our exclusive community, {{username}}!"))
                .andExpect(jsonPath("$.data.templateName").value("VIP Welcome Email"));

        // 2. Preview template output
        com.instagramclone.mail.dto.request.PreviewEmailTemplateRequest previewRequest =
                com.instagramclone.mail.dto.request.PreviewEmailTemplateRequest.builder()
                        .emailType(com.instagramclone.mail.enums.EmailType.WELCOME)
                        .sampleVariables(java.util.Map.of("username", "vip_creator"))
                        .build();

        mockMvc.perform(post(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_TEMPLATE_PREVIEW_PATH)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Welcome to our exclusive community, vip_creator!"))
                .andExpect(jsonPath("$.data.renderedHtml").value(org.hamcrest.Matchers.containsString("vip_creator")));

        // 3. Reset template to default
        mockMvc.perform(post(AdminConstants.BASE_PATH + "/admin/email-templates/reset/WELCOME")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AdminConstants.MSG_EMAIL_TEMPLATE_RESET));

        // Verify template is restored to system default
        mockMvc.perform(get(AdminConstants.BASE_PATH + "/admin/email-templates/WELCOME")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("Welcome Email"));
    }

    @Test
    @DisplayName("Admin Email Templates - Non-admin user accessing template endpoints receives 403 Forbidden")
    void adminEmailTemplates_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest request =
                com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest.builder()
                        .emailType(com.instagramclone.mail.enums.EmailType.WELCOME)
                        .subjectTemplate("Hacked Subject")
                        .bodyHtmlTemplate("<p>Hacked</p>")
                        .build();

        mockMvc.perform(put(AdminConstants.BASE_PATH + AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
