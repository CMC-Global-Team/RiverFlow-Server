package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for time series chart data
 * Used for rendering charts with recharts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTimeSeriesData {

    // Time labels (dates/weeks/months depending on granularity)
    private List<String> labels;

    // Data series
    private List<TimeSeriesPoint> userRegistrations;
    private List<TimeSeriesPoint> revenue;
    private List<TimeSeriesPoint> mindmapCreations;
    private List<TimeSeriesPoint> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String label;
        private Long value;
        private String formattedValue; // For display (e.g., "1,234 VND")
    }

    /**
     * Time period for grouping data
     */
    public enum TimePeriod {
        DAILY, // Last 7 days, grouped by day
        WEEKLY, // Last 4 weeks, grouped by week
        MONTHLY, // Last 12 months, grouped by month
        YEARLY // Last 5 years, grouped by year
    }
}
