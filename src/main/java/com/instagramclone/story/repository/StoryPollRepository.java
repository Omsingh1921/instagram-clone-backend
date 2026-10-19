package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryPollRepository extends JpaRepository<StoryPoll, UUID> {

    Optional<StoryPoll> findByStoryId(UUID storyId);

    @Modifying
    @Query("DELETE FROM StoryPoll sp WHERE sp.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);
}
