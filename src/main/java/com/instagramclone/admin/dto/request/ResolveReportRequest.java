package com.instagramclone.admin.dto.request;

import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class ResolveReportRequest {

    @NotNull(message = ValidationConstants.STATUS_REQUIRED)
    private ReportStatus status;

    @Size(max = ValidationConstants.REPORT_REASON_MAX_LENGTH, message = ValidationConstants.RESOLUTION_NOTES_MAX_1000_MESSAGE)
    private String resolutionNotes;

    @Builder.Default
    private com.instagramclone.admin.enums.ModerationAction action = com.instagramclone.admin.enums.ModerationAction.NO_ACTION;
}
