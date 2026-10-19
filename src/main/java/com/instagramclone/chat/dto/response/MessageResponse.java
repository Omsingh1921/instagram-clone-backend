package com.instagramclone.chat.dto.response;

import com.instagramclone.chat.enums.MessageType;
import com.instagramclone.post.dto.response.PostAuthorResponse;
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
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private PostAuthorResponse sender;
    private String content;
    private MessageType messageType;
    private String mediaUrl;
    private Boolean isDeleted;

    @Builder.Default
    private java.util.List<MessageReactionResponse> reactions = new java.util.ArrayList<>();

    private LocalDateTime createdAt;
}
