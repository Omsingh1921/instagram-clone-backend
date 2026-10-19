package com.instagramclone.user.service.impl;

import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.security.IPrivacyService;
import com.instagramclone.story.constants.StoryConstants;
import com.instagramclone.user.constants.UserConstants;
import com.instagramclone.user.dto.response.CloseFriendResponse;
import com.instagramclone.user.entity.CloseFriend;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.repository.CloseFriendRepository;
import com.instagramclone.user.repository.UserRepository;
import com.instagramclone.user.service.interfaces.ICloseFriendService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloseFriendServiceImpl implements ICloseFriendService {

    private static final Logger log = LoggerFactory.getLogger(CloseFriendServiceImpl.class);

    private final CloseFriendRepository closeFriendRepository;
    private final UserRepository userRepository;
    private final IPrivacyService privacyService;

    @Override
    @Transactional
    public void addCloseFriend(UUID friendId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(friendId)) {
            throw new BadRequestException(StoryConstants.ERR_CLOSE_FRIENDS_CANNOT_ADD_SELF);
        }

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + friendId));

        if (friend.isDeletedOrDeactivated()) {
            throw new ResourceNotFoundException(UserConstants.ERR_USER_NOT_FOUND_ID + friendId);
        }

        if (privacyService.isBlockedBidirectional(currentUserId, friendId)) {
            throw new BadRequestException("Cannot add blocked user to close friends");
        }

        if (closeFriendRepository.existsByUserIdAndFriendId(currentUserId, friendId)) {
            return;
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(UserConstants.ERR_CURRENT_USER_NOT_FOUND));

        CloseFriend closeFriend = CloseFriend.builder()
                .user(currentUser)
                .friend(friend)
                .build();

        closeFriendRepository.save(closeFriend);
        log.info("User {} added {} to close friends", currentUserId, friend.getUsername());
    }

    @Override
    @Transactional
    public void removeCloseFriend(UUID friendId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        closeFriendRepository.deleteByUserIdAndFriendId(currentUserId, friendId);
        log.info("User {} removed {} from close friends", currentUserId, friendId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CloseFriendResponse> getCloseFriends() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<CloseFriend> list = closeFriendRepository.findByUserIdWithFriend(currentUserId);

        return list.stream()
                .filter(cf -> !cf.getFriend().isDeletedOrDeactivated())
                .filter(cf -> !privacyService.isBlockedBidirectional(currentUserId, cf.getFriend().getId()))
                .map(cf -> {
                    User f = cf.getFriend();
                    return CloseFriendResponse.builder()
                            .id(f.getId())
                            .username(f.getUsername())
                            .displayName(f.getProfile() != null && f.getProfile().getDisplayName() != null ? f.getProfile().getDisplayName() : f.getUsername())
                            .avatarUrl(f.getProfile() != null ? f.getProfile().getAvatarUrl() : null)
                            .addedAt(cf.getCreatedAt())
                            .build();
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCloseFriend(UUID userId, UUID friendId) {
        return closeFriendRepository.existsByUserIdAndFriendId(userId, friendId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getCloseFriendIds(UUID userId) {
        return closeFriendRepository.findFriendIdsByUserId(userId);
    }
}
