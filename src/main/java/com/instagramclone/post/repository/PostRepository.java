package com.instagramclone.post.repository;

import com.instagramclone.post.entity.Post;
import com.instagramclone.post.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByIsDeletedFalse();

    long countByUserIdAndIsDeletedFalse(UUID userId);

    default long countByUserId(UUID userId) {
        return countByUserIdAndIsDeletedFalse(userId);
    }

    Page<Post> findByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    default Page<Post> findByUserId(UUID userId, Pageable pageable) {
        return findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    Page<Post> findByUser_IdInAndIsDeletedFalseOrderByCreatedAtDesc(
            @Param("userIds") Collection<UUID> userIds,
            Pageable pageable
    );

    default Page<Post> findByUser_IdInOrderByCreatedAtDesc(
            Collection<UUID> userIds,
            Pageable pageable
    ) {
        return findByUser_IdInAndIsDeletedFalseOrderByCreatedAtDesc(userIds, pageable);
    }

    Optional<Post> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.user.isDeleted = false AND p.user.isActive = true AND p.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE AND (p.user.id = :userId OR (p.user.id IN " +
           "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId) AND " +
           "p.user.id NOT IN (SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :userId) AND " +
           "p.user.id NOT IN (SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :userId))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findTimelinePosts(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.user.isDeleted = false AND p.user.isActive = true AND p.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE AND p.visibility = :visibility AND " +
           "(:excludeUserId IS NULL OR p.user.id != :excludeUserId) AND " +
           "p.user.id NOT IN (SELECT s.user.id FROM UserSettings s WHERE s.isPrivate = true) AND " +
           "(:excludeUserId IS NULL OR (p.user.id NOT IN (SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :excludeUserId) AND " +
           "p.user.id NOT IN (SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :excludeUserId))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findExplorePosts(@Param("visibility") PostVisibility visibility, @Param("excludeUserId") UUID excludeUserId, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.mediaList WHERE p.isDeleted = false AND p.id = :id")
    Optional<Post> findByIdWithMedia(@Param("id") UUID id);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.user.isDeleted = false AND p.user.isActive = true AND p.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE AND p.visibility = :visibility AND " +
           "p.user.id NOT IN (SELECT s.user.id FROM UserSettings s WHERE s.isPrivate = true) AND " +
           "(:viewerId IS NULL OR (p.user.id NOT IN (SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :viewerId) AND " +
           "p.user.id NOT IN (SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :viewerId))) AND " +
           "LOWER(p.caption) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query, @Param("visibility") PostVisibility visibility, @Param("viewerId") UUID viewerId, Pageable pageable);

    default Page<Post> searchPosts(String query, PostVisibility visibility, Pageable pageable) {
        return searchPosts(query, visibility, null, pageable);
    }

    @Query("SELECT DISTINCT p FROM Post p " +
           "JOIN p.mediaList m " +
           "JOIN com.instagramclone.post.entity.PostMediaTag pmt ON pmt.postMedia = m " +
           "WHERE pmt.taggedUser.id = :userId " +
           "AND p.isDeleted = false " +
           "AND p.user.isDeleted = false " +
           "AND p.user.isActive = true " +
           "AND p.user.status = com.instagramclone.user.enums.UserStatus.ACTIVE " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findTaggedPostsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
