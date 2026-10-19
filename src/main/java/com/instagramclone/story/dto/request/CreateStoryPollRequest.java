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
public class CreateStoryPollRequest {

    @NotBlank(message = "Poll question cannot be blank")
    @Size(max = 200, message = "Poll question must not exceed 200 characters")
    private String question;

    @Size(max = 50, message = "Option A must not exceed 50 characters")
    @Builder.Default
    private String optionA = "Yes";

    @Size(max = 50, message = "Option B must not exceed 50 characters")
    @Builder.Default
    private String optionB = "No";
}
