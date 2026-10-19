package com.instagramclone.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private Boolean empty;
    private List<SortMetaData> sorting;

    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return PageResponse.<T>builder().build();
        }

        List<SortMetaData> sorting = (page.getSort() != null && page.getSort().isSorted())
                ? page.getSort().stream()
                        .map(order -> SortMetaData.builder()
                                .field(order.getProperty())
                                .direction(order.getDirection().name())
                                .build())
                        .toList()
                : null;

        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .sorting(sorting)
                .build();
    }

    public boolean isFirstPage() {
        return first;
    }

    public boolean isLastPage() {
        return last;
    }
}
