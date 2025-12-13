package com.riverflow.service.admin;

import com.riverflow.dto.admin.AdminDashboardResponse;
import com.riverflow.model.User;
import com.riverflow.model.UserActivity;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.UserActivityRepository;
import com.riverflow.repository.admin.AdminReportRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Admin Dashboard operations
 * Provides quick overview statistics for both ADMIN and SUPER_ADMIN roles
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminReportRepository adminReportRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserActivityRepository userActivityRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Get dashboard data based on user role
     * 
     * @param currentUserRole The role of the currently authenticated user
     * @return Dashboard data with role-appropriate information
     */
    public AdminDashboardResponse getDashboardData(User.Role currentUserRole) {
        log.info("Fetching dashboard data for role: {}", currentUserRole);

        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

        // Build overview stats (visible to all admins)
        AdminDashboardResponse.OverviewStats overviewStats = buildOverviewStats();

        // Build quick stats (visible to all admins)
        AdminDashboardResponse.QuickStats quickStats = buildQuickStats(startOfToday);

        // Build recent activity (SUPER_ADMIN only)
        List<AdminDashboardResponse.RecentActivityItem> recentActivity = null;
        if (currentUserRole == User.Role.super_admin) {
            recentActivity = getRecentActivity();
        }

        return AdminDashboardResponse.builder()
                .overviewStats(overviewStats)
                .quickStats(quickStats)
                .userRole(currentUserRole.name())
                .recentActivity(recentActivity)
                .build();
    }

    /**
     * Build overview statistics - high-level counts
     */
    private AdminDashboardResponse.OverviewStats buildOverviewStats() {
        Long totalUsers = adminReportRepository.countTotalUsers();
        Long totalMindmaps = countTotalMindmaps();
        Long totalRevenue = paymentTransactionRepository.sumTotalProcessedRevenue();
        Long totalTransactions = paymentTransactionRepository.count();

        return AdminDashboardResponse.OverviewStats.builder()
                .totalUsers(safeOrZero(totalUsers))
                .totalMindmaps(safeOrZero(totalMindmaps))
                .totalRevenue(safeOrZero(totalRevenue))
                .totalTransactions(safeOrZero(totalTransactions))
                .build();
    }

    /**
     * Build quick statistics - today/recent metrics
     */
    private AdminDashboardResponse.QuickStats buildQuickStats(LocalDateTime startOfToday) {
        // User stats
        Long newUsersToday = adminReportRepository.countByCreatedAtAfter(startOfToday);
        Long activeUsersToday = adminReportRepository.countActiveUsersSince(startOfToday);

        // Mindmap stats
        Long newMindmapsToday = countMindmapsAfter(startOfToday);
        Long activeMindmaps = countActiveMindmaps();

        // Payment stats
        Long pendingPayments = paymentTransactionRepository.countByStatus(
                PaymentTransaction.TransactionStatus.pending);
        Long revenueToday = paymentTransactionRepository.sumProcessedRevenueAfter(startOfToday);
        Long transactionsToday = paymentTransactionRepository.countByCreatedAtAfter(startOfToday);

        return AdminDashboardResponse.QuickStats.builder()
                .newUsersToday(safeOrZero(newUsersToday))
                .activeUsersToday(safeOrZero(activeUsersToday))
                .newMindmapsToday(safeOrZero(newMindmapsToday))
                .activeMindmaps(safeOrZero(activeMindmaps))
                .pendingPayments(safeOrZero(pendingPayments))
                .revenueToday(safeOrZero(revenueToday))
                .transactionsToday(safeOrZero(transactionsToday))
                .build();
    }

    /**
     * Get recent activity logs (last 5 entries)
     */
    private List<AdminDashboardResponse.RecentActivityItem> getRecentActivity() {
        try {
            List<UserActivity> activities = userActivityRepository.findAll(
                    PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

            return activities.stream()
                    .map(this::mapToRecentActivityItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error fetching recent activity: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Map UserActivity entity to RecentActivityItem DTO
     */
    private AdminDashboardResponse.RecentActivityItem mapToRecentActivityItem(UserActivity activity) {
        return AdminDashboardResponse.RecentActivityItem.builder()
                .id(activity.getId())
                .action(activity.getActivityType())
                .actor(activity.getUserId() != null ? "User #" + activity.getUserId() : "System")
                .target(activity.getEntityType() + " #" + activity.getEntityId())
                .details(activity.getDetails())
                .timestamp(activity.getCreatedAt())
                .build();
    }

    // ==================== MONGODB HELPER METHODS ====================

    private Long countTotalMindmaps() {
        try {
            return mongoTemplate.count(new Query(), Mindmap.class);
        } catch (Exception e) {
            log.warn("Error counting total mindmaps: {}", e.getMessage());
            return 0L;
        }
    }

    private Long countMindmapsAfter(LocalDateTime startDate) {
        try {
            return mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(startDate)),
                    Mindmap.class);
        } catch (Exception e) {
            log.warn("Error counting mindmaps after {}: {}", startDate, e.getMessage());
            return 0L;
        }
    }

    private Long countActiveMindmaps() {
        try {
            return mongoTemplate.count(
                    Query.query(Criteria.where("status").is("active")),
                    Mindmap.class);
        } catch (Exception e) {
            log.warn("Error counting active mindmaps: {}", e.getMessage());
            return 0L;
        }
    }

    // ==================== UTILITY METHODS ====================

    private Long safeOrZero(Long value) {
        return value != null ? value : 0L;
    }
}
