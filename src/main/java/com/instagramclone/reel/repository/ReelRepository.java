package com.instagramclone.reel.repository;

import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.enums.ReelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReelRepository extends JpaRepository<Reel, UUID> {

    Page<Reel> findByStatus(ReelStatus status, Pageable pageable);

    Page<Reel> findByStatusOrderByCreatedAtDesc(ReelStatus status, Pageable pageable);

    Page<Reel> findByUserIdAndStatus(UUID userId, ReelStatus status, Pageable pageable);

    long countByUserId(UUID userId);

    long countByIsDeletedFalse();

    @Query(value = "SELECT r FROM Reel r WHERE r.status = :status " +
           "AND r.user.isDeleted = false AND r.user.isActive = true AND r.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE " +
           "AND (r.user.id NOT IN (SELECT s.user.id FROM UserSettings s WHERE s.isPrivate = true) " +
           "     OR (:viewerId IS NOT NULL AND (r.user.id = :viewerId OR r.user.id IN (SELECT f.following.id FROM Follow f WHERE f.follower.id = :viewerId)))) " +
           "AND (:viewerId IS NULL OR (r.user.id NOT IN (SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :viewerId) " +
           "     AND r.user.id NOT IN (SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :viewerId))) " +
           "ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM Reel r WHERE r.status = :status " +
           "AND r.user.isDeleted = false AND r.user.isActive = true AND r.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE " +
           "AND (r.user.id NOT IN (SELECT s.user.id FROM UserSettings s WHERE s.isPrivate = true) " +
           "     OR (:viewerId IS NOT NULL AND (r.user.id = :viewerId OR r.user.id IN (SELECT f.following.id FROM Follow f WHERE f.follower.id = :viewerId)))) " +
           "AND (:viewerId IS NULL OR (r.user.id NOT IN (SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :viewerId) " +
           "     AND r.user.id NOT IN (SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :viewerId)))")
    Page<Reel> findFeedReels(@Param("status") ReelStatus status, @Param("viewerId") UUID viewerId, Pageable pageable);
}
