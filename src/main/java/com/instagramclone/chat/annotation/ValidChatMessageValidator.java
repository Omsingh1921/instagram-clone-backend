package com.instagramclone.chat.annotation;

import com.instagramclone.chat.constants.ChatConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for {@link ValidChatMessage}.
 */
public class ValidChatMessageValidator implements ConstraintValidator<ValidChatMessage, String> {

    @Override
    public boolean isValid(String content, ConstraintValidatorContext context) {
        if (content == null || content.isEmpty()) {
            return true;
        }
        return content.length() <= ChatConstants.MAX_MESSAGE_LENGTH;
    }
}
