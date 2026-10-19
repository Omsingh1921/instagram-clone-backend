package com.instagramclone.reel.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import jakarta.validation.constraints.NotBlank;
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
public class CreateReelCommentRequest {

    @NotBlank(message = ValidationConstants.COMMENT_BLANK_MESSAGE)
    @Size(max = ValidationConstants.COMMENT_MAX_LENGTH, message = ValidationConstants.COMMENT_MAX_LENGTH_MESSAGE)
    private String text;
}
