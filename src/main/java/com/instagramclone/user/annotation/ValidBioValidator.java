package com.instagramclone.user.annotation;

import com.instagramclone.common.constants.ValidationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for {@link ValidBio}.
 */
public class ValidBioValidator implements ConstraintValidator<ValidBio, String> {

    @Override
    public boolean isValid(String bio, ConstraintValidatorContext context) {
        if (bio == null || bio.isEmpty()) {
            return true;
        }
        return bio.length() <= ValidationConstants.BIO_MAX_LENGTH;
    }
}
