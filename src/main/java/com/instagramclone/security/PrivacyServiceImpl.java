package com.instagramclone.security;

import com.instagramclone.common.exception.ForbiddenException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.entity.UserSettings;
import com.instagramclone.user.repository.UserBlockRepository;
import com.instagramclone.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements IPrivacyService {

    private final UserBlockRepository userBlockRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final FollowRepository followRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBidirectional(UUID userA, UUID userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return userBlockRepository.existsBlockBetween(userA, userB);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBlockStatus(UUID viewerId, UUID targetUserId) {
        if (viewerId == null || targetUserId == null) {
            return;
        }
        if (isBlockedBidirectional(viewerId, targetUserId)) {
            throw new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + targetUserId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountPrivate(UUID userId) {
        if (userId == null) {
            return false;
        }
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::isPrivate)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewUserContent(UUID viewerId, UUID targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        if (viewerId != null && viewerId.equals(targetUserId)) {
            return true;
        }
        if (viewerId != null && isBlockedBidirectional(viewerId, targetUserId)) {
            return false;
        }
        boolean isPrivate = isAccountPrivate(targetUserId);
        if (!isPrivate) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowingId(viewerId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanViewUserContent(UUID viewerId, UUID targetUserId) {
        if (targetUserId == null) {
            throw new ResourceNotFoundException(UserConstants.ERR_RESOURCE_NOT_FOUND);
        }
        if (viewerId != null && viewerId.equals(targetUserId)) {
            return;
        }
        if (viewerId != null && isBlockedBidirectional(viewerId, targetUserId)) {
            throw new ResourceNotFoundException(UserConstants.ERR_RESOURCE_NOT_FOUND);
        }
        boolean isPrivate = isAccountPrivate(targetUserId);
        if (isPrivate) {
            if (viewerId == null || !followRepository.existsByFollowerIdAndFollowingId(viewerId, targetUserId)) {
                throw new ForbiddenException(UserConstants.ERR_ACCOUNT_PRIVATE);
            }
        }
    }
}
