package com.instagramclone.user.repository;

import com.instagramclone.user.entity.EmailChangeRequest;
import com.instagramclone.user.enums.EmailChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, UUID> {

    Optional<EmailChangeRequest> findByTokenHash(String tokenHash);

    List<EmailChangeRequest> findByUserIdAndStatusAndExpiresAtAfter(
            UUID userId, EmailChangeStatus status, LocalDateTime now);

    default List<EmailChangeRequest> findActivePendingRequests(UUID userId) {
        return findByUserIdAndStatusAndExpiresAtAfter(userId, EmailChangeStatus.PENDING, LocalDateTime.now());
    }

    @Modifying
    @Query("UPDATE EmailChangeRequest r SET r.status = com.instagramclone.user.enums.EmailChangeStatus.CANCELLED WHERE r.user.id = :userId AND r.status = com.instagramclone.user.enums.EmailChangeStatus.PENDING")
    int cancelAllPendingForUser(@Param("userId") UUID userId);
}
