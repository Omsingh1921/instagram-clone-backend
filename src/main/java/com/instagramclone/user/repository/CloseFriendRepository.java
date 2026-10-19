package com.instagramclone.user.repository;

import com.instagramclone.user.entity.CloseFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CloseFriendRepository extends JpaRepository<CloseFriend, UUID> {

    @Query("SELECT cf FROM CloseFriend cf JOIN FETCH cf.friend f LEFT JOIN FETCH f.profile WHERE cf.user.id = :userId ORDER BY cf.createdAt DESC")
    List<CloseFriend> findByUserIdWithFriend(@Param("userId") UUID userId);

    @Query("SELECT cf.friend.id FROM CloseFriend cf WHERE cf.user.id = :userId")
    Set<UUID> findFriendIdsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndFriendId(UUID userId, UUID friendId);

    Optional<CloseFriend> findByUserIdAndFriendId(UUID userId, UUID friendId);

    @Modifying
    @Query("DELETE FROM CloseFriend cf WHERE cf.user.id = :userId AND cf.friend.id = :friendId")
    void deleteByUserIdAndFriendId(@Param("userId") UUID userId, @Param("friendId") UUID friendId);

    @Modifying
    @Query("DELETE FROM CloseFriend cf WHERE cf.user.id = :userId OR cf.friend.id = :userId")
    void deleteByUserIdOrFriendId(@Param("userId") UUID userId);
}
