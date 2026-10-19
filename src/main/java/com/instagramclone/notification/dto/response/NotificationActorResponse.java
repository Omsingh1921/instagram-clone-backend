package com.instagramclone.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationActorResponse {

    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
}
