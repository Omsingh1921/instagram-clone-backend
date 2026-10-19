package com.instagramclone.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailChangeResponse {

    private String message;
    private String currentEmail;
    private String pendingEmail;
    private LocalDateTime expiresAt;
}
