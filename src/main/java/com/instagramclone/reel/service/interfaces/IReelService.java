package com.instagramclone.reel.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.reel.dto.request.CreateReelRequest;
import com.instagramclone.reel.dto.response.ReelResponse;
import org.springframework.data.domain.Pageable;

public interface IReelService {

    ReelResponse createReel(CreateReelRequest request);

    PageResponse<ReelResponse> getReels(Pageable pageable);

    ReelResponse getReelById(UUID reelId);

    PageResponse<ReelResponse> getUserReels(UUID userId, Pageable pageable);

    void deleteReel(UUID reelId);
}
