package com.instagramclone.story.dto.request;

import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.story.constants.StoryConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class CreateHighlightRequest {

    @NotBlank(message = "Highlight title is required")
    @Size(max = StoryConstants.MAX_HIGHLIGHT_TITLE_LENGTH, message = "Highlight title must not exceed " + StoryConstants.MAX_HIGHLIGHT_TITLE_LENGTH + " characters")
    private String title;

    @Size(max = 700, message = "Cover image URL must not exceed 700 characters")
    private String coverImageUrl;

    @NotEmpty(message = StoryConstants.ERR_HIGHLIGHT_STORIES_EMPTY)
    private List<UUID> storyIds;
}
