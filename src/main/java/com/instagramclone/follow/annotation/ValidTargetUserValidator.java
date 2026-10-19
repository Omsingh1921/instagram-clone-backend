package com.instagramclone.follow.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;

/**
 * Constraint validator for {@link ValidTargetUser}.
 */
public class ValidTargetUserValidator implements ConstraintValidator<ValidTargetUser, UUID> {

    @Override
    public boolean isValid(UUID targetId, ConstraintValidatorContext context) {
        return targetId != null;
    }
}
