package com.instagramclone.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private boolean typing;
}
