package com.instagramclone.post.repository;

import com.instagramclone.post.entity.PostMediaTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMediaTagRepository extends JpaRepository<PostMediaTag, UUID> {

    List<PostMediaTag> findByPostMediaId(UUID postMediaId);

    List<PostMediaTag> findByTaggedUserId(UUID taggedUserId);

    boolean existsByPostMediaIdAndTaggedUserId(UUID postMediaId, UUID taggedUserId);

    void deleteByPostMediaId(UUID postMediaId);
}
