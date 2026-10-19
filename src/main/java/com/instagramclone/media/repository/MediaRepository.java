package com.instagramclone.media.repository;

import com.instagramclone.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Media> findByPublicId(String publicId);

    Optional<Media> findBySecureUrl(String secureUrl);

    List<Media> findByOwnerId(UUID ownerId);

    List<Media> findByStatus(String status);
}
