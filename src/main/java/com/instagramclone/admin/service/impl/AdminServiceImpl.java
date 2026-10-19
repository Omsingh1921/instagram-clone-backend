package com.instagramclone.admin.service.impl;

import com.instagramclone.admin.constants.AdminConstants;
import com.instagramclone.admin.dto.request.CreateReportRequest;
import com.instagramclone.admin.dto.request.ResolveReportRequest;
import com.instagramclone.admin.dto.request.UpdateUserRoleRequest;
import com.instagramclone.admin.dto.request.UpdateUserStatusRequest;
import com.instagramclone.admin.dto.response.AdminDashboardStatsResponse;
import com.instagramclone.admin.dto.response.AdminUserResponse;
import com.instagramclone.admin.dto.response.ReportResponse;
import com.instagramclone.admin.entity.Report;
import com.instagramclone.admin.enums.ModerationAction;
import com.instagramclone.admin.enums.ReportStatus;
import com.instagramclone.admin.enums.ReportTargetType;
import com.instagramclone.admin.repository.ReportRepository;
import com.instagramclone.admin.service.interfaces.IAdminService;
import com.instagramclone.audit.service.interfaces.IAuditLogService;
import com.instagramclone.chat.entity.Message;
import com.instagramclone.chat.repository.MessageRepository;
import com.instagramclone.common.exception.BadRequestException;
import com.instagramclone.common.exception.ResourceNotFoundException;
import com.instagramclone.common.response.PageResponse;
import com.instagramclone.common.util.SecurityUtils;
import com.instagramclone.engagement.entity.Comment;
import com.instagramclone.engagement.repository.CommentRepository;
import com.instagramclone.follow.repository.FollowRepository;
import com.instagramclone.notification.enums.NotificationType;
import com.instagramclone.notification.service.interfaces.INotificationService;
import com.instagramclone.post.entity.Post;
import com.instagramclone.post.repository.PostRepository;
import com.instagramclone.reel.entity.Reel;
import com.instagramclone.reel.repository.ReelRepository;
import com.instagramclone.story.entity.Story;
import com.instagramclone.story.repository.StoryRepository;
import com.instagramclone.user.dto.response.UserSummaryResponse;
import com.instagramclone.user.entity.User;
import com.instagramclone.user.enums.Role;
import com.instagramclone.user.enums.UserStatus;
import com.instagramclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final MessageRepository messageRepository;
    private final FollowRepository followRepository;
    private final IAuditLogService auditLogService;
    private final INotificationService notificationService;

    @Override
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_REPORTER_NOT_FOUND));

        UUID targetId = request.getTargetId();
        validateReportTarget(request.getTargetType(), targetId, currentUserId);

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(targetId)
                .reasonCode(request.getReasonCode().trim())
                .description(request.getDescription())
                .status(ReportStatus.OPEN)
                .action(ModerationAction.NO_ACTION)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report {} created by user {} for target {} ID {}",
                saved.getId(), reporter.getUsername(), request.getTargetType(), targetId);

        return toReportResponse(saved);
    }

    private void validateReportTarget(ReportTargetType targetType, UUID targetId, UUID currentUserId) {
        switch (targetType) {
            case USER -> {
                User targetUser = userRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_USER_NOT_FOUND + targetId));
                if (targetUser.getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
            case POST -> {
                Post post = postRepository.findByIdAndIsDeletedFalse(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID: " + targetId));
                if (post.getUser().getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
            case REEL -> {
                Reel reel = reelRepository.findById(targetId)
                        .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                        .orElseThrow(() -> new ResourceNotFoundException("Reel not found with ID: " + targetId));
                if (reel.getUser().getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
            case COMMENT -> {
                Comment comment = commentRepository.findByIdAndIsDeletedFalse(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + targetId));
                if (comment.getUser().getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
            case STORY -> {
                Story story = storyRepository.findById(targetId)
                        .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                        .orElseThrow(() -> new ResourceNotFoundException("Story not found with ID: " + targetId));
                if (story.getUser().getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
            case MESSAGE -> {
                Message message = messageRepository.findById(targetId)
                        .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                        .orElseThrow(() -> new ResourceNotFoundException("Message not found with ID: " + targetId));
                if (message.getSender().getId().equals(currentUserId)) {
                    throw new BadRequestException(AdminConstants.ERR_CANNOT_REPORT_SELF);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getReports(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
        Page<Report> page;
        if (status != null && targetType != null) {
            page = reportRepository.findByStatusAndTargetType(status, targetType, pageable);
        } else if (status != null) {
            page = reportRepository.findByStatus(status, pageable);
        } else if (targetType != null) {
            page = reportRepository.findByTargetType(targetType, pageable);
        } else {
            page = reportRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::toReportResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_REPORT_NOT_FOUND + reportId));
        return toReportResponse(report);
    }

    @Override
    @Transactional
    public ReportResponse resolveReport(UUID reportId, ResolveReportRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User reviewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_REVIEWER_NOT_FOUND));

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_REPORT_NOT_FOUND + reportId));

        ModerationAction action = request.getAction() != null ? request.getAction() : ModerationAction.NO_ACTION;

        report.setStatus(request.getStatus());
        report.setAction(action);
        report.setReviewer(reviewer);
        report.setResolutionNotes(request.getResolutionNotes());
        report.setResolvedAt(LocalDateTime.now());

        // Execute designated moderation action
        if (action == ModerationAction.REMOVE_CONTENT) {
            executeContentTakedown(report.getTargetType(), report.getTargetId(), reviewer);
        } else if (action == ModerationAction.SUSPEND_USER) {
            executeUserSuspension(report.getTargetType(), report.getTargetId(), reviewer);
        } else if (action == ModerationAction.WARN_USER) {
            executeUserWarning(report.getTargetType(), report.getTargetId(), reviewer);
        }

        Report updated = reportRepository.save(report);
        auditLogService.record(reviewer.getId(), "ADMIN_RESOLVE_REPORT", "REPORT", reportId, null,
                "Status: " + request.getStatus() + ", Action: " + action);

        log.info("Report {} resolved with status {} and action {} by reviewer {}",
                reportId, request.getStatus(), action, reviewer.getUsername());

        return toReportResponse(updated);
    }

    private void executeContentTakedown(ReportTargetType targetType, UUID targetId, User reviewer) {
        switch (targetType) {
            case POST -> postRepository.findById(targetId).ifPresent(post -> {
                post.softDelete();
                postRepository.save(post);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_POST", "POST", targetId, null, "Post removed by admin moderation");
                log.info("Post {} soft-deleted by admin {}", targetId, reviewer.getUsername());
            });
            case REEL -> reelRepository.findById(targetId).ifPresent(reel -> {
                reel.softDelete();
                reelRepository.save(reel);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_REEL", "REEL", targetId, null, "Reel removed by admin moderation");
                log.info("Reel {} soft-deleted by admin {}", targetId, reviewer.getUsername());
            });
            case COMMENT -> commentRepository.findById(targetId).ifPresent(comment -> {
                comment.softDelete();
                commentRepository.save(comment);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_COMMENT", "COMMENT", targetId, null, "Comment removed by admin moderation");
                log.info("Comment {} soft-deleted by admin {}", targetId, reviewer.getUsername());
            });
            case STORY -> storyRepository.findById(targetId).ifPresent(story -> {
                story.setExpiresAt(LocalDateTime.now().minusSeconds(1));
                storyRepository.save(story);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_STORY", "STORY", targetId, null, "Story removed by admin moderation");
                log.info("Story {} expired/removed by admin {}", targetId, reviewer.getUsername());
            });
            case MESSAGE -> messageRepository.findById(targetId).ifPresent(msg -> {
                msg.softDelete();
                messageRepository.save(msg);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_MESSAGE", "MESSAGE", targetId, null, "Message removed by admin moderation");
                log.info("Message {} soft-deleted by admin {}", targetId, reviewer.getUsername());
            });
            case USER -> userRepository.findById(targetId).ifPresent(u -> {
                u.setStatus(UserStatus.SUSPENDED);
                u.setIsActive(false);
                userRepository.save(u);
                auditLogService.record(reviewer.getId(), "ADMIN_TAKEDOWN_USER", "USER", targetId, null, "User account suspended by admin moderation");
                log.info("User {} suspended by admin {}", u.getUsername(), reviewer.getUsername());
            });
        }
    }

    private void executeUserSuspension(ReportTargetType targetType, UUID targetId, User reviewer) {
        UUID targetUserId = resolveTargetUserId(targetType, targetId);
        if (targetUserId != null) {
            userRepository.findById(targetUserId).ifPresent(u -> {
                u.setStatus(UserStatus.SUSPENDED);
                u.setIsActive(false);
                userRepository.save(u);
                auditLogService.record(reviewer.getId(), "ADMIN_SUSPEND_USER", "USER", u.getId(), null, "User suspended by admin resolution");
                log.info("User {} (ID: {}) suspended by admin {}", u.getUsername(), u.getId(), reviewer.getUsername());
            });
        }
    }

    private void executeUserWarning(ReportTargetType targetType, UUID targetId, User reviewer) {
        UUID targetUserId = resolveTargetUserId(targetType, targetId);
        if (targetUserId != null) {
            try {
                notificationService.createNotification(
                        targetUserId,
                        reviewer.getId(),
                        NotificationType.SYSTEM,
                        targetType == ReportTargetType.POST ? targetId : null,
                        targetType == ReportTargetType.COMMENT ? targetId : null,
                        targetType == ReportTargetType.REEL ? targetId : null,
                        null
                );
                auditLogService.record(reviewer.getId(), "ADMIN_WARN_USER", "USER", targetUserId, null, "Warning notification sent to user");
                log.info("Warning notification issued to user {} by admin {}", targetUserId, reviewer.getUsername());
            } catch (Exception e) {
                log.warn("Failed to dispatch warning notification to user {}: {}", targetUserId, e.getMessage());
            }
        }
    }

    private UUID resolveTargetUserId(ReportTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case USER -> targetId;
            case POST -> postRepository.findById(targetId).map(p -> p.getUser().getId()).orElse(null);
            case REEL -> reelRepository.findById(targetId).map(r -> r.getUser().getId()).orElse(null);
            case COMMENT -> commentRepository.findById(targetId).map(c -> c.getUser().getId()).orElse(null);
            case STORY -> storyRepository.findById(targetId).map(s -> s.getUser().getId()).orElse(null);
            case MESSAGE -> messageRepository.findById(targetId).map(m -> m.getSender().getId()).orElse(null);
        };
    }

    @Override
    @Transactional
    public void updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BadRequestException(AdminConstants.ERR_CANNOT_MODIFY_OWN_STATUS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_USER_NOT_FOUND + userId));

        user.setStatus(request.getStatus());
        if (request.getStatus() == UserStatus.ACTIVE) {
            user.setIsActive(true);
            user.setIsDeleted(false);
            user.setDeletedAt(null);
        } else if (request.getStatus() == UserStatus.DELETED) {
            user.softDelete();
        } else {
            user.setIsActive(false);
        }
        userRepository.save(user);

        auditLogService.record(currentUserId, "ADMIN_UPDATE_USER_STATUS", "USER", userId, null,
                "New status: " + request.getStatus());
        log.info("User {} status updated to {} by admin {}", user.getUsername(), request.getStatus(), currentUserId);
    }

    @Override
    @Transactional
    public void updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BadRequestException(AdminConstants.ERR_CANNOT_MODIFY_OWN_ROLE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_USER_NOT_FOUND + userId));

        user.setRole(request.getRole());
        userRepository.save(user);

        auditLogService.record(currentUserId, "ADMIN_UPDATE_USER_ROLE", "USER", userId, null,
                "New role: " + request.getRole());
        log.info("User {} role updated to {} by admin {}", user.getUsername(), request.getRole(), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(String query, UserStatus status, Role role, Pageable pageable) {
        Page<User> page = userRepository.findUsersForAdmin(query, status, role, pageable);
        return PageResponse.from(page.map(this::toAdminUserResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long deactivatedUsers = userRepository.countByStatus(UserStatus.DEACTIVATED);

        long totalPosts = postRepository.countByIsDeletedFalse();
        long totalReels = reelRepository.countByIsDeletedFalse();
        long activeStories = storyRepository.countActiveStories(LocalDateTime.now());
        long totalComments = commentRepository.countByIsDeletedFalse();

        long totalReports = reportRepository.count();
        long openReports = reportRepository.countByStatus(ReportStatus.OPEN);
        long reviewedReports = reportRepository.countByStatus(ReportStatus.REVIEWED);
        long resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long dismissedReports = reportRepository.countByStatus(ReportStatus.DISMISSED);

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .deactivatedUsers(deactivatedUsers)
                .totalPosts(totalPosts)
                .totalReels(totalReels)
                .activeStories(activeStories)
                .totalComments(totalComments)
                .totalReports(totalReports)
                .openReports(openReports)
                .reviewedReports(reviewedReports)
                .resolvedReports(resolvedReports)
                .dismissedReports(dismissedReports)
                .build();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        long postsCount = postRepository.countByUserIdAndIsDeletedFalse(user.getId());
        long followersCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());

        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getProfile() != null ? user.getProfile().getDisplayName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .emailVerified(user.getEmailVerified())
                .postsCount(postsCount)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private ReportResponse toReportResponse(Report r) {
        User rep = r.getReporter();
        User rev = r.getReviewer();

        return ReportResponse.builder()
                .id(r.getId())
                .reporter(UserSummaryResponse.builder()
                        .id(rep.getId())
                        .username(rep.getUsername())
                        .displayName(rep.getProfile() != null ? rep.getProfile().getDisplayName() : null)
                        .avatarUrl(rep.getProfile() != null ? rep.getProfile().getAvatarUrl() : null)
                        .build())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reasonCode(r.getReasonCode())
                .description(r.getDescription())
                .status(r.getStatus())
                .action(r.getAction())
                .reviewer(rev != null ? UserSummaryResponse.builder()
                        .id(rev.getId())
                        .username(rev.getUsername())
                        .displayName(rev.getProfile() != null ? rev.getProfile().getDisplayName() : null)
                        .avatarUrl(rev.getProfile() != null ? rev.getProfile().getAvatarUrl() : null)
                        .build() : null)
                .resolutionNotes(r.getResolutionNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
