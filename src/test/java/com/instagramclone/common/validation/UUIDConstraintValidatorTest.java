package com.instagramclone.common.validation;

import com.instagramclone.common.validation.annotation.ValidUUID;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDConstraintValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static class OptionalUuidDto {
        @ValidUUID(required = false)
        private Object id;

        public OptionalUuidDto(Object id) {
            this.id = id;
        }
    }

    private static class RequiredUuidDto {
        @ValidUUID(required = true)
        private Object id;

        public RequiredUuidDto(Object id) {
            this.id = id;
        }
    }

    @Test
    @DisplayName("Optional UUID allows null, empty, and blank strings")
    void optionalUuid_AllowsNullAndEmpty() {
        assertThat(validator.validate(new OptionalUuidDto(null))).isEmpty();
        assertThat(validator.validate(new OptionalUuidDto(""))).isEmpty();
        assertThat(validator.validate(new OptionalUuidDto("   "))).isEmpty();
        assertThat(validator.validate(new OptionalUuidDto(List.of()))).isEmpty();
    }

    @Test
    @DisplayName("Required UUID rejects null, empty, and blank strings")
    void requiredUuid_RejectsNullAndEmpty() {
        assertThat(validator.validate(new RequiredUuidDto(null))).hasSize(1);
        assertThat(validator.validate(new RequiredUuidDto(""))).hasSize(1);
        assertThat(validator.validate(new RequiredUuidDto("   "))).hasSize(1);
        assertThat(validator.validate(new RequiredUuidDto(List.of()))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123e4567-e89b-12d3-a456-426614174000",
            "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            "00000000-0000-0000-0000-000000000000"
    })
    @DisplayName("Valid UUID strings pass validation")
    void validUuidStrings_Pass(String uuidStr) {
        assertThat(validator.validate(new OptionalUuidDto(uuidStr))).isEmpty();
        assertThat(validator.validate(new RequiredUuidDto(uuidStr))).isEmpty();
    }

    @Test
    @DisplayName("Direct java.util.UUID object passes validation")
    void validUuidObject_Passes() {
        UUID uuid = UUID.randomUUID();
        assertThat(validator.validate(new OptionalUuidDto(uuid))).isEmpty();
        assertThat(validator.validate(new RequiredUuidDto(uuid))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-valid-uuid",
            "123e4567-e89b-12d3-a456",
            "123e4567-e89b-12d3-a456-426614174000-extra",
            "zzze4567-e89b-12d3-a456-426614174000"
    })
    @DisplayName("Malformed UUID strings fail validation")
    void malformedUuidStrings_Fail(String invalidUuid) {
        Set<ConstraintViolation<OptionalUuidDto>> violations = validator.validate(new OptionalUuidDto(invalidUuid));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invalid UUID format");
    }

    @Test
    @DisplayName("Valid Collection of UUIDs passes validation")
    void validCollection_Passes() {
        List<String> validList = List.of(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        assertThat(validator.validate(new OptionalUuidDto(validList))).isEmpty();
        assertThat(validator.validate(new RequiredUuidDto(validList))).isEmpty();
    }

    @Test
    @DisplayName("Collection containing an invalid UUID or null element fails")
    void invalidCollection_Fails() {
        List<String> invalidList = List.of(
                UUID.randomUUID().toString(),
                "invalid-uuid-token"
        );
        assertThat(validator.validate(new OptionalUuidDto(invalidList))).hasSize(1);

        List<String> listWithNull = java.util.Arrays.asList(UUID.randomUUID().toString(), null);
        assertThat(validator.validate(new OptionalUuidDto(listWithNull))).hasSize(1);
    }

    @Test
    @DisplayName("Unsupported object types fail safely without unchecked cast exception")
    void unsupportedTypes_FailSafely() {
        assertThat(validator.validate(new OptionalUuidDto(12345))).hasSize(1);
        assertThat(validator.validate(new RequiredUuidDto(12345))).hasSize(1);
    }
}
