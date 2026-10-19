package com.instagramclone.chat.repository;

import com.instagramclone.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    List<MessageReaction> findByMessageId(UUID messageId);

    Optional<MessageReaction> findByMessageIdAndUserId(UUID messageId, UUID userId);

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

    @Modifying
    @Query("DELETE FROM MessageReaction mr WHERE mr.message.id = :messageId AND mr.user.id = :userId")
    void deleteByMessageIdAndUserId(@Param("messageId") UUID messageId, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM MessageReaction mr WHERE mr.message.id = :messageId")
    void deleteByMessageId(@Param("messageId") UUID messageId);
}
