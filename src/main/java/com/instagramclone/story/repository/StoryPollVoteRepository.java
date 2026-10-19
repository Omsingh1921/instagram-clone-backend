package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryPollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryPollVoteRepository extends JpaRepository<StoryPollVote, UUID> {

    Optional<StoryPollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);

    long countByPollIdAndSelectedOption(UUID pollId, int selectedOption);

    long countByPollId(UUID pollId);

    List<StoryPollVote> findByPollId(UUID pollId);

    @Modifying
    @Query("DELETE FROM StoryPollVote spv WHERE spv.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") UUID pollId);

    @Modifying
    @Query("DELETE FROM StoryPollVote spv WHERE spv.poll.story.id = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);
}
