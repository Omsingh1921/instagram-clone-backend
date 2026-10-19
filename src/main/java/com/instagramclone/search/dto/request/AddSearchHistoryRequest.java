package com.instagramclone.search.dto.request;

import com.instagramclone.search.enums.SearchType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSearchHistoryRequest {

    @Size(max = 255, message = "Search query cannot exceed 255 characters")
    private String query;

    private UUID targetUserId;

    @Builder.Default
    private SearchType searchType = SearchType.KEYWORD;
}
