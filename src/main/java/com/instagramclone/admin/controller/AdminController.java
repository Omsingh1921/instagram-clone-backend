package com.instagramclone.admin.controller;

import java.util.UUID;
import com.instagramclone.admin.constants.AdminConstants;
import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.dto.request.UpdateUserRoleRequest;
import com.instagramclone.admin.dto.request.UpdateUserStatusRequest;
import com.instagramclone.admin.dto.response.AdminDashboardStatsResponse;
import com.instagramclone.admin.dto.response.AdminUserResponse;
import com.instagramclone.admin.dto.response.ReportResponse;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.admin.service.interfaces.IAdminService;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminConstants.BASE_PATH)
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;
    private final com.instagramclone.mail.service.interfaces.IDynamicEmailConfigService dynamicEmailConfigService;
    private final com.instagramclone.mail.service.interfaces.IDynamicEmailTemplateService dynamicEmailTemplateService;

    @PostMapping(AdminConstants.REPORTS_PATH)
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody CreateReportRequest request) {
        ReportResponse response = adminService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AdminConstants.MSG_REPORT_SUBMITTED, response));
    }

    @GetMapping(AdminConstants.ADMIN_REPORTS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @PageableDefault(size = AdminConstants.DEFAULT_REPORT_PAGE_SIZE, sort = AdminConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ReportResponse> reports = adminService.getReports(status, targetType, pageable);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_REPORTS_RETRIEVED, reports));
    }

    @GetMapping(AdminConstants.ADMIN_REPORT_BY_ID_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable UUID reportId) {
        ReportResponse response = adminService.getReportById(reportId);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_REPORT_RETRIEVED, response));
    }

    @PutMapping(AdminConstants.ADMIN_REPORT_BY_ID_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        ReportResponse response = adminService.resolveReport(reportId, request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_REPORT_UPDATED, response));
    }

    @GetMapping(AdminConstants.ADMIN_USERS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = AdminConstants.DEFAULT_PAGE_SIZE, sort = AdminConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminUserResponse> users = adminService.getUsers(query, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_USERS_RETRIEVED, users));
    }

    @PutMapping(AdminConstants.ADMIN_USER_STATUS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_USER_STATUS_UPDATED, null));
    }

    @PutMapping(AdminConstants.ADMIN_USER_ROLE_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        adminService.updateUserRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_USER_ROLE_UPDATED, null));
    }

    @GetMapping(AdminConstants.ADMIN_DASHBOARD_STATS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_STATS_RETRIEVED, stats));
    }

    @GetMapping(AdminConstants.ADMIN_STATS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStatsAlias() {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_STATS_RETRIEVED, stats));
    }

    @GetMapping(AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<java.util.List<com.instagramclone.mail.dto.response.DynamicEmailConfigResponse>>> getEmailConfigs() {
        java.util.List<com.instagramclone.mail.dto.response.DynamicEmailConfigResponse> configs = dynamicEmailConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_CONFIGS_RETRIEVED, configs));
    }

    @GetMapping(AdminConstants.ADMIN_EMAIL_CONFIG_BY_CATEGORY_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<com.instagramclone.mail.dto.response.DynamicEmailConfigResponse>> getEmailConfigByCategory(
            @PathVariable com.instagramclone.mail.enums.EmailCategory category) {
        com.instagramclone.mail.dto.response.DynamicEmailConfigResponse config = dynamicEmailConfigService.getConfigByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_CONFIG_RETRIEVED, config));
    }

    @PutMapping(AdminConstants.ADMIN_EMAIL_CONFIGS_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<com.instagramclone.mail.dto.response.DynamicEmailConfigResponse>> updateEmailConfig(
            @Valid @RequestBody com.instagramclone.mail.dto.request.DynamicEmailConfigRequest request) {
        com.instagramclone.mail.dto.response.DynamicEmailConfigResponse config = dynamicEmailConfigService.saveOrUpdateConfig(request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_CONFIG_UPDATED, config));
    }

    @PostMapping(AdminConstants.ADMIN_EMAIL_CONFIG_TEST_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(
            @Valid @RequestBody com.instagramclone.mail.dto.request.TestEmailConfigRequest request) {
        dynamicEmailConfigService.sendTestEmail(request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_TEST_EMAIL_SENT, null));
    }

    @GetMapping(AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<java.util.List<com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse>>> getEmailTemplates() {
        java.util.List<com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse> templates = dynamicEmailTemplateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_TEMPLATES_RETRIEVED, templates));
    }

    @GetMapping(AdminConstants.ADMIN_EMAIL_TEMPLATE_BY_TYPE_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse>> getEmailTemplateByType(
            @PathVariable com.instagramclone.mail.enums.EmailType emailType) {
        com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse template = dynamicEmailTemplateService.getTemplateByType(emailType);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_TEMPLATE_RETRIEVED, template));
    }

    @PutMapping(AdminConstants.ADMIN_EMAIL_TEMPLATES_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse>> updateEmailTemplate(
            @Valid @RequestBody com.instagramclone.mail.dto.request.DynamicEmailTemplateRequest request) {
        com.instagramclone.mail.dto.response.DynamicEmailTemplateResponse template = dynamicEmailTemplateService.saveOrUpdateTemplate(request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_TEMPLATE_UPDATED, template));
    }

    @PostMapping(AdminConstants.ADMIN_EMAIL_TEMPLATE_PREVIEW_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<com.instagramclone.mail.dto.response.PreviewEmailTemplateResponse>> previewEmailTemplate(
            @Valid @RequestBody com.instagramclone.mail.dto.request.PreviewEmailTemplateRequest request) {
        com.instagramclone.mail.dto.response.PreviewEmailTemplateResponse preview = dynamicEmailTemplateService.previewTemplate(request);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_TEMPLATE_PREVIEWED, preview));
    }

    @PostMapping(AdminConstants.ADMIN_EMAIL_TEMPLATE_RESET_PATH)
    @PreAuthorize(AdminConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<ApiResponse<Void>> resetEmailTemplate(
            @PathVariable com.instagramclone.mail.enums.EmailType emailType) {
        dynamicEmailTemplateService.resetTemplateToDefault(emailType);
        return ResponseEntity.ok(ApiResponse.success(AdminConstants.MSG_EMAIL_TEMPLATE_RESET, null));
    }
}
