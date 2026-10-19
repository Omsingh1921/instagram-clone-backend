package com.instagramclone.chat.dto.request;

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
public class AddMessageReactionRequest {

    @NotBlank(message = "Emoji is required")
    @Size(max = 32, message = "Emoji length must not exceed 32 characters")
    private String emoji;
}
