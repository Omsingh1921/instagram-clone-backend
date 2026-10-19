package com.instagramclone.utility;

import com.instagramclone.common.constants.ValidationConstants;
import com.instagramclone.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Reusable pagination construction, bounds clamping, and safe sort-allowlist validation
 * adhering to Section 49.6 (Pagination Utility).
 * Prevents resource exhaustion and unauthorized entity property exposure/injection.
 */
public final class PaginationUtility {

    public static final int DEFAULT_PAGE = ValidationConstants.DEFAULT_PAGE_NUMBER;
    public static final int DEFAULT_SIZE = ValidationConstants.DEFAULT_PAGE_SIZE;
    public static final int MAX_SIZE = ValidationConstants.MAX_PAGE_SIZE;

    private PaginationUtility() {}

    public static Pageable createPageable(Integer page, Integer size) {
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        return PageRequest.of(pageNumber, pageSize);
    }

    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        return PageRequest.of(pageNumber, pageSize, sort != null ? sort : Sort.unsorted());
    }

    public static Pageable createPageable(Integer page, Integer size, Sort sort, Set<String> allowedProperties) {
        validateSort(sort, allowedProperties);
        return createPageable(page, size, sort);
    }

    public static Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection, Set<String> allowedProperties) {
        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.isBlank()) {
            String trimmedSortBy = sortBy.trim();
            if (allowedProperties != null && !allowedProperties.isEmpty() && !allowedProperties.contains(trimmedSortBy)) {
                throw new BadRequestException("Invalid sort property '" + trimmedSortBy + "'. Allowed sort properties: " + allowedProperties);
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (sortDirection != null && !sortDirection.isBlank()) {
                String trimmedDir = sortDirection.trim();
                if ("desc".equalsIgnoreCase(trimmedDir)) {
                    direction = Sort.Direction.DESC;
                } else if ("asc".equalsIgnoreCase(trimmedDir)) {
                    direction = Sort.Direction.ASC;
                } else {
                    throw new BadRequestException("Invalid sort direction '" + sortDirection + "'. Must be ASC or DESC");
                }
            }
            sort = Sort.by(direction, trimmedSortBy);
        }

        return createPageable(page, size, sort);
    }

    public static void validateSort(Sort sort, Set<String> allowedProperties) {
        if (sort == null || sort.isUnsorted() || allowedProperties == null || allowedProperties.isEmpty()) {
            return;
        }

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!allowedProperties.contains(property)) {
                throw new BadRequestException("Invalid sort property '" + property + "'. Allowed sort properties: " + allowedProperties);
            }
        }
    }

    public static Pageable clamp(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        }
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_SIZE);
        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }

    public static Pageable clamp(Pageable pageable, Set<String> allowedProperties) {
        Pageable clamped = clamp(pageable);
        validateSort(clamped.getSort(), allowedProperties);
        return clamped;
    }
}
