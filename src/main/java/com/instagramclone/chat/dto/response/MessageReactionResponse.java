package com.instagramclone.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionResponse {

    private UUID userId;
    private String username;
    private String displayName;
    private String emoji;
    private LocalDateTime createdAt;
}
