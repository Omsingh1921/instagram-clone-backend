package com.instagramclone.user;


import java.util.UUID;
import com.instagramclone.BaseIntegrationTest;
import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SettingsAdminIntegrationTest extends BaseIntegrationTest {

    private User regularUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        regularUser = userRepository.save(User.builder()
                .username("regular_user")
                .email("regular@example.com")
                .passwordHash(passwordEncoder.encode("CurrentPass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .role(Role.ROLE_USER)
                .build());

        adminUser = userRepository.save(User.builder()
                .username("admin_user")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .role(Role.ROLE_ADMIN)
                .build());

        userToken = jwtTokenProvider.generateTokenForUser(regularUser.getId(), regularUser.getUsername(), regularUser.getEmail());
        adminToken = jwtTokenProvider.generateTokenForUser(adminUser.getId(), adminUser.getUsername(), adminUser.getEmail());
    }

    @Test
    @DisplayName("Settings - Fetch and update privacy and notification settings")
    void settings_ShouldUpdateSuccessfully() throws Exception {
        // Fetch initial
        mockMvc.perform(get("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy.privateAccount").value(false));

        // Update to private
        UpdateSettingsRequest updateReq = UpdateSettingsRequest.builder()
                .privateAccount(true)
                .notifyLikes(false)
                .build();

        mockMvc.perform(put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy.privateAccount").value(true))
                .andExpect(jsonPath("$.data.notifications.likes").value(false));
    }

    @Test
    @DisplayName("Password Change - Should validate old password and reject bad credentials")
    void changePassword_ShouldEnforceSecurityRules() throws Exception {
        // Wrong old password -> 400
        ChangePasswordRequest wrongOld = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("BrandNewPass123!")
                .confirmPassword("BrandNewPass123!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOld)))
                .andExpect(status().isBadRequest());

        // Correct old password -> success
        ChangePasswordRequest legit = ChangePasswordRequest.builder()
                .currentPassword("CurrentPass123!")
                .newPassword("BrandNewPass123!")
                .confirmPassword("BrandNewPass123!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legit)))
                .andExpect(status().isOk());

        User reloaded = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("BrandNewPass123!", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Admin - Regular user cannot access admin report queue; Admin can resolve report")
    void adminEndpoints_ShouldEnforceRoleSecurity() throws Exception {
        // User creates a report
        CreateReportRequest reportReq = CreateReportRequest.builder()
                .targetType(ReportTargetType.USER)
                .targetId(adminUser.getId())
                .reasonCode("SPAM")
                .description("Suspicious automated activity")
                .build();

        String reportJson = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reportId = UUID.fromString(objectMapper.readTree(reportJson).get("data").get("id").asText());

        // Regular user cannot view admin reports -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Admin can view and resolve
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reasonCode").value("SPAM"));

        ResolveReportRequest resolveReq = ResolveReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .resolutionNotes("Verified, no violation found")
                .build();

        mockMvc.perform(put("/api/v1/admin/reports/{reportId}", reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }
}
