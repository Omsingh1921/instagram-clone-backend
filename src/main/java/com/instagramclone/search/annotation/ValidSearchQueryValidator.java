package com.instagramclone.search.annotation;

import com.instagramclone.search.constants.SearchConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for {@link ValidSearchQuery}.
 */
public class ValidSearchQueryValidator implements ConstraintValidator<ValidSearchQuery, String> {

    @Override
    public boolean isValid(String query, ConstraintValidatorContext context) {
        if (query == null) {
            return false;
        }
        String cleaned = query.trim();
        return cleaned.length() >= SearchConstants.MIN_QUERY_LENGTH
                && cleaned.length() <= SearchConstants.MAX_QUERY_LENGTH;
    }
}
