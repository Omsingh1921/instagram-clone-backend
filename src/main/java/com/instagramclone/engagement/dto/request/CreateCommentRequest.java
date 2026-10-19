package com.instagramclone.engagement.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.common.validation.annotation.ValidUUID;
import jakarta.validation.constraints.NotBlank;
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
public class CreateCommentRequest {

    @ValidUUID(required = false, message = "Parent comment ID must be a valid UUID")
    private UUID parentCommentId;

    @NotBlank(message = ValidationConstants.COMMENT_REQUIRED)
    @Size(max = ValidationConstants.COMMENT_MAX_LENGTH, message = ValidationConstants.COMMENT_MAX_LENGTH_MESSAGE)
    private String text;
}
