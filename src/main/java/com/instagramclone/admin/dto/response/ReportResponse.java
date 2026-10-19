package com.instagramclone.admin.dto.response;

import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private UUID id;
    private UserSummaryResponse reporter;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reasonCode;
    private String description;
    private ReportStatus status;
    private UserSummaryResponse reviewer;
    private com.instagramclone.admin.enums.ModerationAction action;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
