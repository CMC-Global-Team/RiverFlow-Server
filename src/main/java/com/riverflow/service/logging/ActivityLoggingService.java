package com.riverflow.service.logging;

import com.riverflow.dto.logging.ActivityLogResponse;
import com.riverflow.dto.logging.LogStatisticsResponse;
import com.riverflow.model.logging.ActivityLog;
import com.riverflow.repository.logging.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for logging activities to MongoDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLoggingService {

    private final ActivityLogRepository activityLogRepository;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log a generic activity
     */
    public ActivityLog log(ActivityLog activityLog) {
        if (activityLog.getTimestamp() == null) {
            activityLog.setTimestamp(LocalDateTime.now());
        }
        ActivityLog saved = activityLogRepository.save(activityLog);
        log.info("[ACTIVITY LOG] {} - {} by {} ({})",
                activityLog.getCategory(),
                activityLog.getAction(),
                activityLog.getActorEmail(),
                activityLog.getActorRole());
        return saved;
    }

    /**
     * Log a successful payment
     */
    public void logPaymentSuccess(Long userId, String userEmail, Long transactionId,
            Long amount, Long credits) {
        String details = String.format(
                "{\"transactionId\":%d,\"amount\":%d,\"credits\":%d}",
                transactionId, amount, credits);

        ActivityLog activityLog = ActivityLog.builder()
                .actorId(userId)
                .actorEmail(userEmail)
                .actorRole("USER")
                .action(ActivityLog.Action.PAYMENT_SUCCESS.name())
                .category(ActivityLog.Category.PAYMENT.name())
                .targetId(String.valueOf(transactionId))
                .targetType("PAYMENT_TRANSACTION")
                .details(details)
                .build();

        log(activityLog);
    }

    /**
     * Log a user management action
     */
    public void logUserManagementAction(Long actorId, String actorEmail, String actorRole,
            String action, Long targetUserId, String details) {
        ActivityLog activityLog = ActivityLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(action)
                .category(ActivityLog.Category.USER_MANAGEMENT.name())
                .targetId(String.valueOf(targetUserId))
                .targetType("USER")
                .details(details)
                .build();

        log(activityLog);
    }

    /**
     * Log a user management action with IP address
     */
    public void logUserManagementAction(Long actorId, String actorEmail, String actorRole,
            String action, Long targetUserId, String details,
            String ipAddress, String userAgent) {
        ActivityLog activityLog = ActivityLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(action)
                .category(ActivityLog.Category.USER_MANAGEMENT.name())
                .targetId(String.valueOf(targetUserId))
                .targetType("USER")
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        log(activityLog);
    }

    /**
     * Log a payment management action
     */
    public void logPaymentManagementAction(Long actorId, String actorEmail, String actorRole,
            String action, Long paymentId, String details) {
        ActivityLog activityLog = ActivityLog.builder()
                .actorId(actorId)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(action)
                .category(ActivityLog.Category.PAYMENT_MANAGEMENT.name())
                .targetId(String.valueOf(paymentId))
                .targetType("PAYMENT_TRANSACTION")
                .details(details)
                .build();

        log(activityLog);
    }

    /**
     * Get logs with search and filter
     */
    public Page<ActivityLogResponse> getLogs(String search, String category, String action,
            String actorRole, LocalDateTime startDate,
            LocalDateTime endDate, int page, int size,
            String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, "timestamp");
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchPattern = (search != null && !search.isEmpty()) ? search : "";

        Page<ActivityLog> logs = activityLogRepository.searchLogs(
                searchPattern,
                (category != null && !category.isEmpty()) ? category : null,
                (action != null && !action.isEmpty()) ? action : null,
                (actorRole != null && !actorRole.isEmpty()) ? actorRole : null,
                startDate,
                endDate,
                pageable);

        return logs.map(this::mapToResponse);
    }

    /**
     * Get log by ID
     */
    public ActivityLogResponse getLogById(String id) {
        return activityLogRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));
    }

    /**
     * Get log statistics
     */
    public LogStatisticsResponse getStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last7d = now.minusDays(7);

        long totalLogs = activityLogRepository.count();
        long logsLast24h = activityLogRepository.countByTimestampBetween(last24h, now);
        long logsLast7d = activityLogRepository.countByTimestampBetween(last7d, now);

        // Breakdown by category
        Map<String, Long> byCategory = new HashMap<>();
        for (ActivityLog.Category cat : ActivityLog.Category.values()) {
            byCategory.put(cat.name(), activityLogRepository.countByCategory(cat.name()));
        }

        // Breakdown by action (top actions)
        Map<String, Long> byAction = new HashMap<>();
        for (ActivityLog.Action act : ActivityLog.Action.values()) {
            long count = activityLogRepository.countByAction(act.name());
            if (count > 0) {
                byAction.put(act.name(), count);
            }
        }

        // Breakdown by actor role
        Map<String, Long> byActorRole = new HashMap<>();
        byActorRole.put("USER", activityLogRepository.countByCategory("PAYMENT")); // Approximate
        byActorRole.put("ADMIN", activityLogRepository.countByCategoryAndTimestampBetween(
                ActivityLog.Category.USER_MANAGEMENT.name(), LocalDateTime.MIN, now));
        byActorRole.put("SUPER_ADMIN", 0L); // Will be calculated more accurately if needed

        return LogStatisticsResponse.builder()
                .totalLogs(totalLogs)
                .logsLast24h(logsLast24h)
                .logsLast7d(logsLast7d)
                .byCategory(byCategory)
                .byAction(byAction)
                .byActorRole(byActorRole)
                .build();
    }

    /**
     * Map ActivityLog to ActivityLogResponse
     */
    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorEmail(log.getActorEmail())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .category(log.getCategory())
                .targetId(log.getTargetId())
                .targetType(log.getTargetType())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .formattedTimestamp(log.getTimestamp() != null ? log.getTimestamp().format(DISPLAY_FORMAT) : null)
                .actionDescription(getActionDescription(log.getAction()))
                .build();
    }

    /**
     * Get human-readable action description
     */
    private String getActionDescription(String action) {
        if (action == null)
            return "Unknown action";

        return switch (action) {
            case "USER_CREATE" -> "Created a new user";
            case "USER_UPDATE" -> "Updated user information";
            case "USER_DELETE" -> "Deleted a user";
            case "USER_SOFT_DELETE" -> "Soft deleted a user";
            case "USER_HARD_DELETE" -> "Permanently deleted a user";
            case "USER_RESTORE" -> "Restored a deleted user";
            case "CREDIT_UPDATE" -> "Updated user credits";
            case "PASSWORD_CHANGE" -> "Changed user password";
            case "PAYMENT_SUCCESS" -> "Payment completed successfully";
            case "PAYMENT_FAILED" -> "Payment failed";
            case "PAYMENT_STATUS_UPDATE" -> "Updated payment status";
            case "LOGIN" -> "User logged in";
            case "LOGOUT" -> "User logged out";
            case "SYSTEM_CONFIG_UPDATE" -> "Updated system configuration";
            default -> action.replace("_", " ").toLowerCase();
        };
    }
}
