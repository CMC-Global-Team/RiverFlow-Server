package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment statistics (super admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatisticsResponse {

    // Overall statistics
    private Long totalPayments;
    private Long totalAmount;

    // Status counts
    private Long pendingCount;
    private Long processedCount;
    private Long matchedCount;
    private Long ignoredCount;
    private Long invalidCount;

    // Time-based statistics
    private Long todayPayments;
    private Long todayAmount;

    private Long weekPayments;
    private Long weekAmount;

    private Long monthPayments;
    private Long monthAmount;
}
