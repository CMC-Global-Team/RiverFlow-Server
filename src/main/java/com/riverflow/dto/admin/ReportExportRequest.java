package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for report export parameters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportRequest {

    /**
     * Type of report to export
     */
    private ReportType reportType;

    /**
     * Start date for the report period
     */
    private LocalDate startDate;

    /**
     * End date for the report period
     */
    private LocalDate endDate;

    /**
     * Export format
     */
    private ExportFormat format;

    /**
     * Time granularity for aggregation
     */
    private TimeGranularity granularity;

    public enum ReportType {
        USERS, // User statistics report
        REVENUE, // Revenue/payment report
        MINDMAPS, // Mindmap statistics report
        ALL // Combined report
    }

    public enum ExportFormat {
        CSV,
        JSON,
        XLSX
    }

    public enum TimeGranularity {
        DAY,
        WEEK,
        MONTH
    }
}
