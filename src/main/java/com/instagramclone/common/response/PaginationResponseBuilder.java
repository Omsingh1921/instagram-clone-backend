package com.instagramclone.common.response;

import org.springframework.data.domain.Page;

/**
 * Reusable builder utility for constructing {@link PageResponse} from Spring Data {@link Page}.
 */
public final class PaginationResponseBuilder {

    private PaginationResponseBuilder() {
        // Prevent instantiation
    }

    public static <T> PageResponse<T> build(Page<T> pageData) {
        return PageResponse.from(pageData);
    }
}
