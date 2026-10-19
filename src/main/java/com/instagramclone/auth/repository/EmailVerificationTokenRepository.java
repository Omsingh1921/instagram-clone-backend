package com.instagramclone.auth.repository;

import com.instagramclone.auth.entity.EmailVerificationToken;
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
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(UUID userId);

    Optional<EmailVerificationToken> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateAllUserTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now OR t.usedAt IS NOT NULL")
    int purgeExpiredOrUsedTokens(@Param("now") LocalDateTime now);
}
