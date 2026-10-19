package com.instagramclone.admin.dto.request;

import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.common.validation.annotation.ValidUUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.instagramclone.common.constants.ValidationConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class CreateReportRequest {

    @NotNull(message = "Target type is required")
    private ReportTargetType targetType;

    @NotNull(message = "Target ID is required")
    @ValidUUID(required = true, message = "Target ID must be a valid UUID")
    private UUID targetId;

    @NotBlank(message = ValidationConstants.REPORT_REASON_REQUIRED)
    @Size(max = 50, message = "Reason code cannot exceed 50 characters")
    private String reasonCode;

    @Size(max = ValidationConstants.REPORT_REASON_MAX_LENGTH, message = ValidationConstants.REPORT_REASON_MAX_LENGTH_MESSAGE)
    private String description;
}
