package com.instagramclone.admin.common;

import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Common representation of an admin moderation action or report summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditSummary {

    private UUID reportId;
    private UUID reporterId;
    private ReportTargetType targetType;
    private UUID targetId;
    private ReportStatus status;
    private String reason;
    private Instant resolvedAt;
}
