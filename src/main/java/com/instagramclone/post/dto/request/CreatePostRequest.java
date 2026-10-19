package com.instagramclone.post.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.post.enums.PostVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import com.instagramclone.common.constants.ValidationConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class CreatePostRequest {

    @Size(max = ValidationConstants.CAPTION_MAX_LENGTH, message = ValidationConstants.CAPTION_MAX_LENGTH_MESSAGE)
    private String caption;

    @jakarta.validation.constraints.NotNull(message = "Post visibility is required")
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @NotEmpty(message = "At least one media item is required")
    @Size(max = ValidationConstants.MAX_POST_MEDIA_ITEMS, message = "Post cannot contain more than 10 media items")
    @Valid
    @Builder.Default
    private List<PostMediaRequest> media = new ArrayList<>();
}
