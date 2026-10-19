package com.instagramclone.reel.repository;

import com.instagramclone.reel.entity.ReelLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReelLikeRepository extends JpaRepository<ReelLike, UUID> {

    Optional<ReelLike> findByUserIdAndReelId(UUID userId, UUID reelId);

    boolean existsByUserIdAndReelId(UUID userId, UUID reelId);

    boolean existsByReelIdAndUserId(UUID reelId, UUID userId);

    long countByReelId(UUID reelId);

    Page<ReelLike> findByReelId(UUID reelId, Pageable pageable);

    void deleteByReelId(UUID reelId);
}
