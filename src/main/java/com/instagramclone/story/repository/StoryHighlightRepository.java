package com.instagramclone.story.repository;

import com.instagramclone.story.entity.StoryHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryHighlightRepository extends JpaRepository<StoryHighlight, UUID> {

    List<StoryHighlight> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT h FROM StoryHighlight h LEFT JOIN FETCH h.items items LEFT JOIN FETCH items.story WHERE h.id = :id")
    Optional<StoryHighlight> findByIdWithItems(@Param("id") UUID id);
}
