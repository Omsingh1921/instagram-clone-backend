package com.instagramclone.admin.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    @NotNull(message = ValidationConstants.STATUS_REQUIRED)
    private UserStatus status;
}
