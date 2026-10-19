package com.instagramclone.user.service.interfaces;

import com.instagramclone.user.dto.response.CloseFriendResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ICloseFriendService {

    void addCloseFriend(UUID friendId);

    void removeCloseFriend(UUID friendId);

    List<CloseFriendResponse> getCloseFriends();

    boolean isCloseFriend(UUID userId, UUID friendId);

    Set<UUID> getCloseFriendIds(UUID userId);
}
