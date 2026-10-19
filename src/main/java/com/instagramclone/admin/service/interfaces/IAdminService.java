package com.instagramclone.admin.service.interfaces;

import java.util.UUID;
import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.dto.request.UpdateUserRoleRequest;
import com.instagramclone.admin.dto.request.UpdateUserStatusRequest;
import com.instagramclone.admin.dto.response.AdminDashboardStatsResponse;
import com.instagramclone.admin.dto.response.AdminUserResponse;
import com.instagramclone.admin.dto.response.ReportResponse;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import org.springframework.data.domain.Pageable;

public interface IAdminService {

    ReportResponse createReport(CreateReportRequest request);

    PageResponse<ReportResponse> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    ReportResponse getReportById(UUID reportId);

    ReportResponse resolveReport(UUID reportId, ResolveReportRequest request);

    void updateUserStatus(UUID userId, UpdateUserStatusRequest request);

    void updateUserRole(UUID userId, UpdateUserRoleRequest request);

    PageResponse<AdminUserResponse> getUsers(String query, UserStatus status, Role role, Pageable pageable);

    AdminDashboardStatsResponse getDashboardStats();
}
