package com.instagramclone.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured sorting metadata indicating field and sort direction (ASC/DESC).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortMetaData {

    private String field;
    private String direction;
}
