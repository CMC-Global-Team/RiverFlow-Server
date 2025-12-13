package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Admin Dashboard statistics
 * Accessible by both ADMIN and SUPER_ADMIN roles
 * Provides quick overview metrics (different from detailed Reports)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // Overview statistics (visible to all admins)
    private OverviewStats overviewStats;

    // Quick statistics for today/recent (visible to all admins)
    private QuickStats quickStats;

    // Current user's role (for UI differentiation)
    private String userRole;

    // Recent activity logs (SUPER_ADMIN only - null for regular admin)
    private List<RecentActivityItem> recentActivity;

    /**
     * Overview statistics - high-level counts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Long totalUsers;
        private Long totalMindmaps;
        private Long totalRevenue;
        private Long totalTransactions;
    }

    /**
     * Quick statistics - recent/today metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickStats {
        // User stats
        private Long newUsersToday;
        private Long activeUsersToday;

        // Mindmap stats
        private Long newMindmapsToday;
        private Long activeMindmaps;

        // Payment stats
        private Long pendingPayments;
        private Long revenueToday;
        private Long transactionsToday;
    }

    /**
     * Recent activity item for activity log preview
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private Long id;
        private String action;
        private String actor;
        private String target;
        private String details;
        private LocalDateTime timestamp;
    }
}
