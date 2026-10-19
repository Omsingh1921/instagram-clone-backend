package com.instagramclone.auth.repository;

import com.instagramclone.auth.entity.SignupVerificationTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignupVerificationTicketRepository extends JpaRepository<SignupVerificationTicket, UUID> {

    Optional<SignupVerificationTicket> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM SignupVerificationTicket t WHERE t.tokenHash = :tokenHash")
    Optional<SignupVerificationTicket> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE SignupVerificationTicket t SET t.consumedAt = :now WHERE t.email = :email AND t.consumedAt IS NULL")
    int invalidateExistingTicketsForEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SignupVerificationTicket t WHERE t.expiresAt < :now OR t.consumedAt IS NOT NULL")
    int purgeExpiredOrConsumedTickets(@Param("now") LocalDateTime now);
}
