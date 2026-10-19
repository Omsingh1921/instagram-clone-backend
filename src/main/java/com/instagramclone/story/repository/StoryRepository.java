package com.instagramclone.story.repository;

import com.instagramclone.story.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {

    List<Story> findByUserIdAndExpiresAtAfterOrderByCreatedAtAsc(UUID userId, LocalDateTime now);

    List<Story> findByUserIdInAndExpiresAtAfterOrderByCreatedAtAsc(Collection<UUID> userIds, LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.expiresAt > :now")
    long countActiveStories(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.user.id IN :userIds AND s.expiresAt > :now " +
           "AND s.user.isDeleted = false AND s.user.isActive = true AND s.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE " +
           "ORDER BY s.createdAt ASC")
    List<Story> findActiveStoriesByUserIds(@Param("userIds") Collection<UUID> userIds, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.user.id = :userId AND s.expiresAt > :now " +
           "AND s.user.isDeleted = false AND s.user.isActive = true AND s.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE " +
           "ORDER BY s.createdAt ASC")
    List<Story> findActiveStoriesByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    List<Story> findByExpiresAtBefore(LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.expiresAt < :now AND s.id NOT IN (SELECT shi.story.id FROM StoryHighlightItem shi)")
    List<Story> findExpiredStoriesNotInHighlights(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Story s WHERE s.expiresAt < :now")
    int deleteExpiredStories(@Param("now") LocalDateTime now);
}
