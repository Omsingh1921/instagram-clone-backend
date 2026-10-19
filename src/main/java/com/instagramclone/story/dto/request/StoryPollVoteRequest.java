package com.instagramclone.story.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryPollVoteRequest {

    @Min(value = 1, message = "Option must be 1 or 2")
    @Max(value = 2, message = "Option must be 1 or 2")
    private int selectedOption;
}
