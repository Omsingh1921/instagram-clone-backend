package com.instagramclone.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {

    private Boolean privateAccount;
    private Boolean notifyLikes;
    private Boolean notifyComments;
    private Boolean notifyFollows;
    private Boolean notifyMessages;
}
