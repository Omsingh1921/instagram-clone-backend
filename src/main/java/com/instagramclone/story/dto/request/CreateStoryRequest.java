package com.instagramclone.story.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.post.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateStoryRequest {

    @NotBlank(message = ValidationConstants.MEDIA_URL_REQUIRED)
    @Pattern(regexp = ValidationConstants.MEDIA_URL_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.MEDIA_URL_MAX_LENGTH, message = ValidationConstants.MEDIA_URL_MAX_LENGTH_MESSAGE)
    private String mediaUrl;

    @NotNull(message = ValidationConstants.MEDIA_TYPE_REQUIRED)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Size(max = 500, message = "Story caption must not exceed 500 characters")
    private String caption;

    @Builder.Default
    private boolean closeFriendsOnly = false;

    private CreateStoryPollRequest poll;

    @Builder.Default
    private java.util.List<StoryMentionRequest> mentions = new java.util.ArrayList<>();
}
