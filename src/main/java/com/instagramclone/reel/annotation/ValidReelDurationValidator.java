package com.instagramclone.reel.annotation;

import com.instagramclone.reel.constants.ReelConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidReelDuration}.
 */
public class ValidReelDurationValidator implements ConstraintValidator<ValidReelDuration, Integer> {

    @Override
    public boolean isValid(Integer durationSeconds, ConstraintValidatorContext context) {
        if (durationSeconds == null) {
            return true;
        }
        return durationSeconds >= ReelConstants.MIN_DURATION_SECONDS
                && durationSeconds <= ReelConstants.MAX_DURATION_SECONDS;
    }
}
