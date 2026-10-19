package com.instagramclone.feed.service.interfaces;

import com.instagramclone.common.response.PageResponse;
import com.instagramclone.post.dto.response.PostResponse;
import org.springframework.data.domain.Pageable;

public interface IFeedService {

    PageResponse<PostResponse> getHomeFeed(Pageable pageable);

    PageResponse<PostResponse> getExploreFeed(Pageable pageable);
}
