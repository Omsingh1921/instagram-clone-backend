package com.instagramclone.reel.repository;

import com.instagramclone.reel.entity.ReelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReelViewRepository extends JpaRepository<ReelView, UUID> {

    boolean existsByReelIdAndUserId(UUID reelId, UUID userId);

    long countByReelId(UUID reelId);

    void deleteByReelId(UUID reelId);
}
