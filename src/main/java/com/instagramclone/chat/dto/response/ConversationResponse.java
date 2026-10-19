package com.instagramclone.chat.dto.response;

import com.instagramclone.chat.enums.ConversationType;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;
    private ConversationType type;
    private String name;
    private String avatarUrl;
    private UUID creatorId;

    @Builder.Default
    private List<UserSummaryResponse> members = new ArrayList<>();

    private MessageResponse lastMessage;
    private long unreadCount;
    private LocalDateTime updatedAt;
}
