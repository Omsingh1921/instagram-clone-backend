package com.instagramclone.chat.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.ValidUUID;
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
public class CreateConversationRequest {

    @NotNull(message = ValidationConstants.RECIPIENT_ID_REQUIRED)
    @ValidUUID(required = true, message = ValidationConstants.RECIPIENT_ID_UUID_MESSAGE)
    private UUID recipientId;
}
