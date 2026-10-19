package com.instagramclone.story.dto.response;

import com.instagramclone.user.dto.response.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoriesResponse {

    private UserSummaryResponse user;

    @Builder.Default
    private List<StoryResponse> stories = new ArrayList<>();

    private boolean allViewed;
    private boolean hasCloseFriendsStories;
}
