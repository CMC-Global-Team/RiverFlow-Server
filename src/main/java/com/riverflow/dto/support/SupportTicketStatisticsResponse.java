package com.riverflow.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for support ticket statistics (admin dashboard)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketStatisticsResponse {

    private Long totalTickets;
    private Long openTickets;
    private Long resolvedTickets;
    private Long closedTickets;

    // Counts by status
    private Map<String, Long> ticketsByStatus;

    // Counts by priority
    private Map<String, Long> ticketsByPriority;

    // Counts by category
    private Map<String, Long> ticketsByCategory;

    // Unassigned tickets count
    private Long unassignedTickets;
}
