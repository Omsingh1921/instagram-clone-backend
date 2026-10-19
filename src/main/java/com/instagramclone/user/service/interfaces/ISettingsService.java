package com.instagramclone.user.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.dto.response.UserSettingsResponse;
import org.springframework.data.domain.Pageable;

public interface ISettingsService {

    UserSettingsResponse getUserSettings();

    UserSettingsResponse updateSettings(UpdateSettingsRequest request);

    void changePassword(ChangePasswordRequest request);

    void blockUser(UUID targetUserId);

    void unblockUser(UUID targetUserId);

    PageResponse<FollowUserResponse> getBlockedUsers(Pageable pageable);
}
