package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for comprehensive report statistics (SUPER_ADMIN only)
 * Contains user, mindmap, and revenue statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatisticsResponse {

    // User Statistics
    private UserStatistics userStats;

    // Mindmap Statistics
    private MindmapStatistics mindmapStats;

    // Revenue Statistics
    private RevenueStatistics revenueStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        private Long totalUsers;
        private Long activeUsers;
        private Long suspendedUsers;
        private Long deletedUsers;

        // New users by time period
        private Long newUsersToday;
        private Long newUsersThisWeek;
        private Long newUsersThisMonth;

        // By role
        private Long adminCount;
        private Long superAdminCount;
        private Long regularUserCount;

        // Growth percentage (compared to previous period)
        private Double weeklyGrowthPercent;
        private Double monthlyGrowthPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MindmapStatistics {
        private Long totalMindmaps;
        private Long activeMindmaps;
        private Long archivedMindmaps;
        private Long deletedMindmaps;

        // New mindmaps by time period
        private Long newMindmapsToday;
        private Long newMindmapsThisWeek;
        private Long newMindmapsThisMonth;

        // By visibility
        private Long publicMindmaps;
        private Long privateMindmaps;

        // By AI generation
        private Long aiGeneratedMindmaps;

        // Growth percentage
        private Double weeklyGrowthPercent;
        private Double monthlyGrowthPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStatistics {
        private Long totalRevenue;
        private Long totalTransactions;

        // Revenue by time period
        private Long revenueToday;
        private Long revenueThisWeek;
        private Long revenueThisMonth;

        // Transactions by time period
        private Long transactionsToday;
        private Long transactionsThisWeek;
        private Long transactionsThisMonth;

        // Average transaction value
        private Double averageTransactionValue;

        // Growth percentage
        private Double weeklyGrowthPercent;
        private Double monthlyGrowthPercent;
    }
}
