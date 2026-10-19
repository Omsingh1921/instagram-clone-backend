package com.instagramclone.story.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that story media URL is present and not exceeding bounds.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidStoryMediaValidator.class)
@Documented
public @interface ValidStoryMedia {

    String message() default com.instagramclone.story.constants.StoryConstants.MSG_INVALID_STORY_MEDIA;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
