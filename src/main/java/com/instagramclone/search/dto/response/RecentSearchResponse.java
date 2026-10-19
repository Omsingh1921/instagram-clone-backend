package com.instagramclone.search.dto.response;

import com.instagramclone.search.enums.SearchType;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentSearchResponse {

    private UUID id;
    private String query;
    private SearchType searchType;
    private UserSummaryResponse targetUser;
    private LocalDateTime createdAt;
}
