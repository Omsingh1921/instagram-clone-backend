package com.instagramclone.auth.repository;

import com.instagramclone.auth.entity.RefreshToken;
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
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActivityAtDesc(UUID userId, LocalDateTime now);

    Optional<RefreshToken> findBySessionIdAndRevokedAtIsNull(UUID sessionId);

    List<RefreshToken> findBySessionId(UUID sessionId);

    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.sessionId = :sessionId AND rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.sessionId = :sessionId AND rt.revokedAt IS NULL")
    int revokeAllTokensBySessionId(@Param("sessionId") UUID sessionId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user.id = :userId AND rt.sessionId != :currentSessionId AND rt.revokedAt IS NULL")
    int revokeOtherSessions(@Param("userId") UUID userId, @Param("currentSessionId") UUID currentSessionId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.id = :id AND rt.revokedAt IS NULL")
    int markRevokedIfActive(@Param("id") UUID id, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.lastActivityAt = :now WHERE rt.sessionId = :sessionId AND rt.revokedAt IS NULL")
    int updateLastActivityBySessionId(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllUserTokens(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revokedAt IS NOT NULL")
    int purgeExpiredOrRevokedTokens(@Param("now") LocalDateTime now);
}
