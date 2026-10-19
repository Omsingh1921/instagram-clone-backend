package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryHighlightItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryHighlightItemRepository extends JpaRepository<StoryHighlightItem, UUID> {

    List<StoryHighlightItem> findByHighlightIdOrderByOrderIndexAsc(UUID highlightId);

    boolean existsByStoryId(UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryHighlightItem shi WHERE shi.highlight.id = :highlightId")
    void deleteByHighlightId(@Param("highlightId") UUID highlightId);

    @Modifying
    @Query("DELETE FROM StoryHighlightItem shi WHERE shi.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);
}
