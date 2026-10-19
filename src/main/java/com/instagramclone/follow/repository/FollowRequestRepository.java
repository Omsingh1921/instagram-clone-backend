package com.instagramclone.follow.repository;

import com.instagramclone.follow.entity.FollowRequest;
import com.instagramclone.follow.enums.FollowRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRequestRepository extends JpaRepository<FollowRequest, UUID> {

    Optional<FollowRequest> findByRequesterIdAndTargetId(UUID requesterId, UUID targetId);

    boolean existsByRequesterIdAndTargetIdAndStatus(UUID requesterId, UUID targetId, FollowRequestStatus status);

    Page<FollowRequest> findByTargetIdAndStatusOrderByCreatedAtDesc(UUID targetId, FollowRequestStatus status, Pageable pageable);

    long countByTargetIdAndStatus(UUID targetId, FollowRequestStatus status);

    void deleteByRequesterIdAndTargetId(UUID requesterId, UUID targetId);
}
