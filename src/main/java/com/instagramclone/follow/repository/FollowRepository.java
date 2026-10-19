package com.instagramclone.follow.repository;

import com.instagramclone.follow.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // Active counts: filter out deleted and deactivated accounts
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :followingId AND f.follower.isDeleted = false AND f.follower.isActive = true AND f.follower.status = com.instagramclone.user.enums.UserStatus.ACTIVE")
    long countActiveFollowersByFollowingId(@Param("followingId") UUID followingId);

    default long countByFollowingId(UUID followingId) {
        return countActiveFollowersByFollowingId(followingId);
    }

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :followerId AND f.following.isDeleted = false AND f.following.isActive = true AND f.following.status = com.instagramclone.user.enums.UserStatus.ACTIVE")
    long countActiveFollowingByFollowerId(@Param("followerId") UUID followerId);

    default long countByFollowerId(UUID followerId) {
        return countActiveFollowingByFollowerId(followerId);
    }

    // Active lists: filter out deleted and deactivated accounts
    @Query("SELECT f FROM Follow f WHERE f.following.id = :followingId AND f.follower.isDeleted = false AND f.follower.isActive = true AND f.follower.status = com.instagramclone.user.enums.UserStatus.ACTIVE")
    Page<Follow> findActiveFollowersByFollowingId(@Param("followingId") UUID followingId, Pageable pageable);

    default Page<Follow> findByFollowingId(UUID followingId, Pageable pageable) {
        return findActiveFollowersByFollowingId(followingId, pageable);
    }

    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId AND f.following.isDeleted = false AND f.following.isActive = true AND f.following.status = com.instagramclone.user.enums.UserStatus.ACTIVE")
    Page<Follow> findActiveFollowingByFollowerId(@Param("followerId") UUID followerId, Pageable pageable);

    default Page<Follow> findByFollowerId(UUID followerId, Pageable pageable) {
        return findActiveFollowingByFollowerId(followerId, pageable);
    }

    // Unfiltered queries for Admin
    @Query("SELECT f FROM Follow f WHERE f.following.id = :followingId")
    Page<Follow> findAllByFollowingId(@Param("followingId") UUID followingId, Pageable pageable);

    @Query("SELECT f FROM Follow f WHERE f.follower.id = :followerId")
    Page<Follow> findAllByFollowerId(@Param("followerId") UUID followerId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :followingId")
    long countAllByFollowingId(@Param("followingId") UUID followingId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :followerId")
    long countAllByFollowerId(@Param("followerId") UUID followerId);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId AND f.following.isDeleted = false AND f.following.isActive = true AND f.following.status = com.instagramclone.user.enums.UserStatus.ACTIVE")
    List<UUID> findFollowingIdsByFollowerId(@Param("followerId") UUID followerId);
}
