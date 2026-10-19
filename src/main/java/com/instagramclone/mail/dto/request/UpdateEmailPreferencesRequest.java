package com.instagramclone.mail.dto.request;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.validation.annotation.TrimValidator;
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
public class UpdateEmailPreferencesRequest {

    private Boolean emailSocialFollows;
    private Boolean emailSocialLikes;
    private Boolean emailSocialComments;
    private Boolean emailMessageReminders;
    private Boolean emailActivityDigest;
    private Boolean emailProductAnnouncements;

    @Pattern(
            regexp = ValidationConstants.DIGEST_FREQUENCY_REGEX,
            message = ValidationConstants.DIGEST_FREQUENCY_MESSAGE
    )
    @Size(max = ValidationConstants.DIGEST_FREQUENCY_MAX_LENGTH, message = ValidationConstants.DIGEST_FREQUENCY_MAX_LENGTH_MESSAGE)
    private String digestFrequency;
}
