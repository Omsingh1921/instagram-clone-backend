package com.instagramclone.user.repository;

import com.instagramclone.user.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Page<UserBlock> findByBlockerId(UUID blockerId, Pageable pageable);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) > 0 FROM UserBlock b WHERE " +
           "(b.blocker.id = :userA AND b.blocked.id = :userB) OR " +
           "(b.blocker.id = :userB AND b.blocked.id = :userA)")
    boolean existsBlockBetween(@org.springframework.data.repository.query.Param("userA") UUID userA, @org.springframework.data.repository.query.Param("userB") UUID userB);
}
