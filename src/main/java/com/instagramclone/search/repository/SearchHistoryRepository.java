package com.instagramclone.search.repository;

import com.instagramclone.search.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.id = :id AND sh.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
