package com.instagramclone.user.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Common lightweight representation of a User across user and dependent modules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isPrivate;
    private boolean isVerified;
}
