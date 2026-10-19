package com.instagramclone.post.annotation;

import com.instagramclone.post.constants.PostConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidCaption}.
 */
public class ValidCaptionValidator implements ConstraintValidator<ValidCaption, String> {

    @Override
    public boolean isValid(String caption, ConstraintValidatorContext context) {
        if (caption == null || caption.isEmpty()) {
            return true;
        }
        return caption.length() <= PostConstants.MAX_CAPTION_LENGTH;
    }
}
