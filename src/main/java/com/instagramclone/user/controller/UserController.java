package com.instagramclone.user.controller;


import java.util.UUID;
import com.instagramclone.common.constants.ApiConstants;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.dto.request.UpdateAvatarRequest;
import com.instagramclone.user.dto.request.UpdateProfileRequest;
import com.instagramclone.user.dto.response.UserProfileResponse;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.service.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UserConstants.USERS_BASE_PATH)
@RequiredArgsConstructor
@Tag(name = UserConstants.TAG_USER_PROFILE, description = UserConstants.TAG_USER_PROFILE_DESCRIPTION)
public class UserController {

    private final IUserService userService;

    @GetMapping(UserConstants.PATH_ME)
    @Operation(summary = "Get current authenticated user profile", description = "Retrieves profile and identity details of the user in the security context")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        UserProfileResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping(UserConstants.PATH_ME)
    @Operation(summary = "Soft delete current authenticated user account", description = "Deactivates account with a 15-day recovery window")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(UserConstants.MSG_ACCOUNT_DELETED_SUCCESS, null));
    }

    @PostMapping(UserConstants.PATH_ME + "/deactivate")
    @Operation(summary = "Deactivate current authenticated user account", description = "Temporarily deactivates account until next login")
    public ResponseEntity<ApiResponse<Void>> deactivateCurrentUser() {
        userService.deactivateCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully. You can reactivate anytime by logging in.", null));
    }

    @GetMapping(UserConstants.PATH_USER_BY_ID)
    @Operation(summary = "Get public user profile by ID", description = "Retrieves safe public profile information for any user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable UUID userId) {
        UserProfileResponse response = userService.getUserProfileById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(UserConstants.PATH_USER_BY_USERNAME)
    @Operation(summary = "Get public user profile by username", description = "Retrieves safe public profile information by username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfileByUsername(@PathVariable String username) {
        UserProfileResponse response = userService.getUserProfileByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping(UserConstants.PATH_ME_PROFILE)
    @Operation(summary = "Update current user profile", description = "Updates displayName, bio, and websiteUrl for the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(UserConstants.MSG_PROFILE_UPDATED_SUCCESS, response));
    }

    @PatchMapping(UserConstants.PATH_ME_AVATAR)
    @Operation(summary = "Update current user avatar", description = "Updates avatarUrl reference for the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        UserProfileResponse response = userService.updateAvatar(request);
        return ResponseEntity.ok(ApiResponse.success(UserConstants.MSG_AVATAR_UPDATED_SUCCESS, response));
    }

    @GetMapping(UserConstants.PATH_SEARCH)
    @Operation(summary = "Search users", description = "Performs paginated search against usernames and display names")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> searchUsers(
            @RequestParam(required = false, defaultValue = ApiConstants.EMPTY_STRING) String q,
            @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE_10, sort = UserConstants.SORT_BY_USERNAME, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserSummaryResponse> response = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
