package com.instagramclone.common.validation.annotation;

import com.instagramclone.common.constants.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a field, parameter, or collection element represents a valid RFC 4122 UUID.
 * Supports {@link CharSequence}, {@link java.util.UUID}, and {@link Iterable} / {@link java.util.Collection}.
 */
@Documented
@Constraint(validatedBy = UUIDConstraintValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUUID {

    boolean required() default false;

    String message() default ValidationConstants.INVALID_UUID_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
