package com.instagramclone.chat.repository;

import com.instagramclone.chat.entity.Conversation;
import com.instagramclone.chat.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c JOIN c.members m1 JOIN c.members m2 " +
           "WHERE c.type = :type AND m1.user.id = :user1Id AND m2.user.id = :user2Id")
    Optional<Conversation> findDirectConversationBetweenUsers(
            @Param("type") ConversationType type,
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    Page<Conversation> findByMembers_User_IdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    default Page<Conversation> findConversationsByUserId(UUID userId, Pageable pageable) {
        return findByMembers_User_IdOrderByUpdatedAtDesc(userId, pageable);
    }
}
