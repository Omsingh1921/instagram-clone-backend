package com.instagramclone.reel.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that reel duration is between 3 and 90 seconds.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReelDurationValidator.class)
@Documented
public @interface ValidReelDuration {

    String message() default com.instagramclone.reel.constants.ReelConstants.MSG_INVALID_REEL_DURATION;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
