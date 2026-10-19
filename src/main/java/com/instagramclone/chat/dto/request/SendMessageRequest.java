package com.instagramclone.chat.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
import com.instagramclone.chat.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TrimValidator
public class SendMessageRequest {

    @Size(max = ValidationConstants.MESSAGE_CONTENT_MAX_LENGTH, message = ValidationConstants.MESSAGE_CONTENT_MAX_LENGTH_MESSAGE)
    private String content;

    @NotNull(message = ValidationConstants.MESSAGE_TYPE_REQUIRED)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Pattern(regexp = ValidationConstants.MEDIA_URL_OR_EMPTY_REGEX, message = ValidationConstants.MEDIA_URL_INVALID_MESSAGE)
    @Size(max = ValidationConstants.MEDIA_URL_MAX_LENGTH, message = ValidationConstants.MEDIA_URL_MAX_LENGTH_MESSAGE)
    private String mediaUrl;
}
