package com.instagramclone.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long deactivatedUsers;

    private long totalPosts;
    private long totalReels;
    private long activeStories;
    private long totalComments;

    private long totalReports;
    private long openReports;
    private long reviewedReports;
    private long resolvedReports;
    private long dismissedReports;
}
