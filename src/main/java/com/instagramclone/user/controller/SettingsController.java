package com.instagramclone.user.controller;

import com.instagramclone.common.constants.ApiConstants;
import com.instagramclone.common.response.ApiResponse;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.follow.dto.response.FollowUserResponse;
import com.instagramclone.mail.dto.request.UpdateEmailPreferencesRequest;
import com.instagramclone.mail.dto.response.EmailPreferencesResponse;
import com.instagramclone.mail.service.interfaces.IEmailPreferenceService;
import com.instagramclone.ratelimit.RateLimit;
import com.instagramclone.user.constants.SettingsConstants;
import com.instagramclone.auth.dto.request.ChangePasswordRequest;
import com.instagramclone.user.dto.request.InitiateEmailChangeRequest;
import com.instagramclone.user.dto.request.UpdateSettingsRequest;
import com.instagramclone.user.dto.response.EmailChangeResponse;
import com.instagramclone.user.dto.response.UserSettingsResponse;
import com.instagramclone.user.service.interfaces.IEmailChangeService;
import com.instagramclone.user.service.interfaces.ISettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(SettingsConstants.USERS_BASE_PATH)
@RequiredArgsConstructor
public class SettingsController {

    private final ISettingsService settingsService;
    private final IEmailPreferenceService emailPreferenceService;
    private final IEmailChangeService emailChangeService;

    @RateLimit(category = SettingsConstants.RATE_LIMIT_EMAIL_CHANGE, limit = 3, windowSeconds = 60)
    @PostMapping(SettingsConstants.PATH_ME_EMAIL)
    public ResponseEntity<ApiResponse<EmailChangeResponse>> initiateEmailChange(
            @Valid @RequestBody InitiateEmailChangeRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        EmailChangeResponse response = emailChangeService.initiateEmailChange(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_EMAIL_CHANGE_DISPATCHED, response));
    }

    @DeleteMapping(SettingsConstants.PATH_ME_EMAIL)
    public ResponseEntity<ApiResponse<Void>> cancelPendingEmailChange() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        emailChangeService.cancelPendingEmailChange(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_EMAIL_CHANGE_CANCELLED, null));
    }

    @GetMapping(SettingsConstants.PATH_ME_EMAIL_PREFERENCES)
    public ResponseEntity<ApiResponse<EmailPreferencesResponse>> getEmailPreferences() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        EmailPreferencesResponse response = emailPreferenceService.getPreferences(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_EMAIL_PREFERENCES_RETRIEVED, response));
    }

    @PutMapping(SettingsConstants.PATH_ME_EMAIL_PREFERENCES)
    public ResponseEntity<ApiResponse<EmailPreferencesResponse>> updateEmailPreferences(
            @Valid @RequestBody UpdateEmailPreferencesRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        EmailPreferencesResponse response = emailPreferenceService.updatePreferences(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_EMAIL_PREFERENCES_UPDATED, response));
    }

    @GetMapping(SettingsConstants.PATH_ME_SETTINGS)
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings() {
        UserSettingsResponse response = settingsService.getUserSettings();
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_SETTINGS_FETCHED_SUCCESS, response));
    }

    @PutMapping(SettingsConstants.PATH_ME_SETTINGS)
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request) {
        UserSettingsResponse response = settingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_SETTINGS_UPDATED_SUCCESS, response));
    }

    @RateLimit(category = SettingsConstants.RATE_LIMIT_PASSWORD, limit = 5, windowSeconds = 60)
    @PutMapping(SettingsConstants.PATH_ME_PASSWORD)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        settingsService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_PASSWORD_CHANGED_SUCCESS, null));
    }

    @PostMapping(SettingsConstants.PATH_USER_BLOCK)
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable UUID userId) {
        settingsService.blockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_USER_BLOCKED_SUCCESS, null));
    }

    @DeleteMapping(SettingsConstants.PATH_USER_BLOCK)
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable UUID userId) {
        settingsService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_USER_UNBLOCKED_SUCCESS, null));
    }

    @GetMapping(SettingsConstants.PATH_ME_BLOCKS)
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getBlockedUsers(
            @PageableDefault(size = ApiConstants.DEFAULT_PAGE_SIZE_20, sort = ApiConstants.SORT_BY_CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FollowUserResponse> blocked = settingsService.getBlockedUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(SettingsConstants.MSG_BLOCKED_USERS_RETRIEVED, blocked));
    }
}
