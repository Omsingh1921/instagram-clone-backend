package com.instagramclone.user.service.interfaces;


import java.util.UUID;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.user.dto.request.UpdateAvatarRequest;
import com.instagramclone.user.dto.request.UpdateProfileRequest;
import com.instagramclone.user.dto.response.UserProfileResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface IUserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse getUserProfileById(UUID userId);

    UserProfileResponse getUserProfileByUsername(String username);

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    UserProfileResponse updateAvatar(UpdateAvatarRequest request);

    PageResponse<UserSummaryResponse> searchUsers(String query, Pageable pageable);

    void deleteCurrentUser();

    void deleteUser(UUID userId);

    void deactivateCurrentUser();

    void deactivateUser(UUID userId);
}
