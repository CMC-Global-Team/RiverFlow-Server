package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for package statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageStatsResponse {
    
    /**
     * Total number of packages
     */
    private Long totalPackages;
    
    /**
     * Number of active packages
     */
    private Long activePackages;
    
    /**
     * Total number of active subscribers across all packages
     */
    private Long activeSubscribers;
    
    /**
     * Monthly Recurring Revenue (estimated)
     */
    private BigDecimal monthlyRecurringRevenue;
    
    /**
     * Conversion rate percentage
     */
    private Double conversionRate;
    
    /**
     * Growth percentage compared to last month
     */
    private Double growthPercentage;
}

