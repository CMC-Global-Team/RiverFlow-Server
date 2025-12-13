package com.riverflow.dto.logging;

import lombok.*;

import java.util.Map;

/**
 * Response DTO for log statistics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogStatisticsResponse {

    /**
     * Total number of logs
     */
    private long totalLogs;

    /**
     * Logs count in the last 24 hours
     */
    private long logsLast24h;

    /**
     * Logs count in the last 7 days
     */
    private long logsLast7d;

    /**
     * Breakdown by category
     */
    private Map<String, Long> byCategory;

    /**
     * Breakdown by action
     */
    private Map<String, Long> byAction;

    /**
     * Breakdown by actor role
     */
    private Map<String, Long> byActorRole;
}
