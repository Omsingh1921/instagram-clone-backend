package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryLikeRepository extends JpaRepository<StoryLike, UUID> {

    Optional<StoryLike> findByStoryIdAndUserId(UUID storyId, UUID userId);

    boolean existsByStoryIdAndUserId(UUID storyId, UUID userId);

    long countByStoryId(UUID storyId);

    List<StoryLike> findByStoryId(UUID storyId);

    List<StoryLike> findByStoryIdAndUserIdIn(UUID storyId, Collection<UUID> userIds);

    @Modifying
    @Query("DELETE FROM StoryLike sl WHERE sl.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryLike sl WHERE sl.story.expiresAt < :now")
    int deleteByStoryExpiresAtBefore(@Param("now") LocalDateTime now);
}
