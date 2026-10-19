package com.instagramclone.post.repository;

import com.instagramclone.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {

    List<PostMedia> findByPostIdOrderByDisplayOrderAsc(UUID postId);

    void deleteByPostId(UUID postId);
}
