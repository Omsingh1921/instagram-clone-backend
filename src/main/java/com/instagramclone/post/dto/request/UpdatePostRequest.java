package com.instagramclone.post.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.post.enums.PostVisibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.instagramclone.common.constants.ValidationConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class UpdatePostRequest {

    @Size(max = ValidationConstants.CAPTION_MAX_LENGTH, message = ValidationConstants.CAPTION_MAX_LENGTH_MESSAGE)
    private String caption;

    private PostVisibility visibility;
}
