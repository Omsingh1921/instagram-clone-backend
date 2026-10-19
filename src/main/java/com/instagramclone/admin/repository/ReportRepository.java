package com.instagramclone.admin.repository;

import com.instagramclone.admin.entity.Report;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByTargetType(ReportTargetType targetType, Pageable pageable);

    Page<Report> findByStatusAndTargetType(ReportStatus status, ReportTargetType targetType, Pageable pageable);

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
