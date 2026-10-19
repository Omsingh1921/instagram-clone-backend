package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {

    boolean existsByStoryIdAndUserId(UUID storyId, UUID userId);

    long countByStoryId(UUID storyId);

    List<StoryView> findByStoryIdOrderByViewedAtDesc(UUID storyId);

    @Query("SELECT sv FROM StoryView sv JOIN FETCH sv.user u LEFT JOIN FETCH u.profile WHERE sv.story.id = :storyId ORDER BY sv.viewedAt DESC")
    List<StoryView> findByStoryIdWithUser(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryView sv WHERE sv.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryView sv WHERE sv.story.expiresAt < :now")
    int deleteByStoryExpiresAtBefore(@Param("now") LocalDateTime now);
}
