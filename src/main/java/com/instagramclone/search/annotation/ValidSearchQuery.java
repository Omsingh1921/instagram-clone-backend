package com.instagramclone.search.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that search queries meet length constraints and are not blank.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSearchQueryValidator.class)
@Documented
public @interface ValidSearchQuery {

    String message() default com.instagramclone.search.constants.SearchConstants.MSG_INVALID_SEARCH_QUERY;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
