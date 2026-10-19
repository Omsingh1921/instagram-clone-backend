package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoryMentionRepository extends JpaRepository<StoryMention, UUID> {

    List<StoryMention> findByStoryId(UUID storyId);

    @Query("SELECT sm FROM StoryMention sm JOIN FETCH sm.mentionedUser u LEFT JOIN FETCH u.profile WHERE sm.story.id = :storyId")
    List<StoryMention> findByStoryIdWithUser(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryMention sm WHERE sm.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryMention sm WHERE sm.story.expiresAt < :now")
    int deleteByStoryExpiresAtBefore(@Param("now") LocalDateTime now);
}
