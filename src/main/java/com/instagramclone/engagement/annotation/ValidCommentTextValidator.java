package com.instagramclone.engagement.annotation;

import com.instagramclone.engagement.constants.EngagementConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for {@link ValidCommentText}.
 */
public class ValidCommentTextValidator implements ConstraintValidator<ValidCommentText, String> {

    @Override
    public boolean isValid(String text, ConstraintValidatorContext context) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return !trimmed.isEmpty() && trimmed.length() <= EngagementConstants.MAX_COMMENT_LENGTH;
    }
}
