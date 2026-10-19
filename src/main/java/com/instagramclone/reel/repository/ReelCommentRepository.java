package com.instagramclone.reel.repository;

import com.instagramclone.reel.entity.ReelComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReelCommentRepository extends JpaRepository<ReelComment, UUID> {

    Page<ReelComment> findByReelIdOrderByCreatedAtAsc(UUID reelId, Pageable pageable);

    long countByReelId(UUID reelId);

    void deleteByReelId(UUID reelId);
}
