package com.instagramclone.utility;

import com.instagramclone.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationUtilityEnhancedTest {

    private static final Set<String> ALLOWED_FIELDS = Set.of("createdAt", "username", "likeCount");

    @Test
    @DisplayName("createPageable applies default page and size on null input")
    void createPageable_Defaults() {
        Pageable p = PaginationUtility.createPageable(null, null);
        assertThat(p.getPageNumber()).isEqualTo(PaginationUtility.DEFAULT_PAGE);
        assertThat(p.getPageSize()).isEqualTo(PaginationUtility.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("createPageable clamps oversized requested page sizes to MAX_SIZE (100)")
    void createPageable_ClampsMaxSize() {
        Pageable p = PaginationUtility.createPageable(2, 500);
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(PaginationUtility.MAX_SIZE);
    }

    @Test
    @DisplayName("createPageable clamps negative page number to 0")
    void createPageable_ClampsNegativePage() {
        Pageable p = PaginationUtility.createPageable(-5, 10);
        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("createPageable with Sort passes when property is in allowlist")
    void createPageable_WithAllowedSort() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable p = PaginationUtility.createPageable(0, 20, sort, ALLOWED_FIELDS);
        assertThat(p.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("createPageable with Sort rejects unpermitted property")
    void createPageable_WithDisallowedSort_Throws() {
        Sort sort = Sort.by(Sort.Direction.ASC, "passwordHash");
        assertThatThrownBy(() -> PaginationUtility.createPageable(0, 20, sort, ALLOWED_FIELDS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort property 'passwordHash'");
    }

    @Test
    @DisplayName("createPageable with string parameters parses valid sortBy and direction")
    void createPageable_WithStringSort_Valid() {
        Pageable p = PaginationUtility.createPageable(1, 15, "username", "desc", ALLOWED_FIELDS);
        assertThat(p.getPageNumber()).isEqualTo(1);
        assertThat(p.getPageSize()).isEqualTo(15);
        assertThat(p.getSort().getOrderFor("username")).isNotNull();
        assertThat(p.getSort().getOrderFor("username").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("createPageable with string parameters rejects invalid sort property")
    void createPageable_WithStringSort_InvalidProperty_Throws() {
        assertThatThrownBy(() -> PaginationUtility.createPageable(0, 20, "secretField", "asc", ALLOWED_FIELDS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort property 'secretField'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalidDir", "up", "1", "sideways"})
    @DisplayName("createPageable with string parameters rejects malformed sort direction")
    void createPageable_WithStringSort_InvalidDirection_Throws(String invalidDir) {
        assertThatThrownBy(() -> PaginationUtility.createPageable(0, 20, "createdAt", invalidDir, ALLOWED_FIELDS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort direction");
    }

    @Test
    @DisplayName("clamp handles null pageable and enforces sort allowlist")
    void clamp_ValidatesAndClamps() {
        Pageable clampedNull = PaginationUtility.clamp(null, ALLOWED_FIELDS);
        assertThat(clampedNull.getPageNumber()).isEqualTo(0);
        assertThat(clampedNull.getPageSize()).isEqualTo(20);

        Pageable valid = PageRequest.of(1, 20, Sort.by("createdAt"));
        assertThatCode(() -> PaginationUtility.clamp(valid, ALLOWED_FIELDS)).doesNotThrowAnyException();

        Pageable invalid = PageRequest.of(1, 20, Sort.by("unauthorizedField"));
        assertThatThrownBy(() -> PaginationUtility.clamp(invalid, ALLOWED_FIELDS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort property 'unauthorizedField'");
    }
}
