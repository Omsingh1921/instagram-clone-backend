package com.instagramclone.story.dto.request;

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
public class StoryReplyRequest {

    @NotBlank(message = "Reply message cannot be blank")
    @Size(max = 1000, message = "Reply message must not exceed 1000 characters")
    private String content;
}
