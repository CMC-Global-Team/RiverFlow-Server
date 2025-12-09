package com.riverflow.controller.admin;

import com.riverflow.dto.admin.ReportExportRequest;
import com.riverflow.dto.admin.ReportStatisticsResponse;
import com.riverflow.dto.admin.ReportTimeSeriesData;
import com.riverflow.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for Admin Report endpoints
 * Accessible only by SUPER_ADMIN role
 */
@Slf4j
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * Get comprehensive statistics
     * GET /api/admin/reports/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ReportStatisticsResponse> getStatistics() {
        log.info("Fetching report statistics");
        ReportStatisticsResponse statistics = adminReportService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get time series data for charts
     * GET /api/admin/reports/time-series
     */
    @GetMapping("/time-series")
    public ResponseEntity<ReportTimeSeriesData> getTimeSeriesData(
            @RequestParam(defaultValue = "DAILY") ReportTimeSeriesData.TimePeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching time series data for period: {}", period);
        ReportTimeSeriesData timeSeriesData = adminReportService.getTimeSeriesData(period, startDate, endDate);
        return ResponseEntity.ok(timeSeriesData);
    }

    /**
     * Export report as specified format
     * POST /api/admin/reports/export
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody ReportExportRequest request) {
        log.info("Exporting report: type={}, format={}", request.getReportType(), request.getFormat());

        byte[] reportBytes = adminReportService.exportReport(request);

        String filename = generateFilename(request);
        MediaType mediaType = getMediaType(request.getFormat());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportBytes);
    }

    /**
     * Generate filename for export
     */
    private String generateFilename(ReportExportRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportType = request.getReportType() != null ? request.getReportType().name().toLowerCase() : "all";
        String extension = request.getFormat() != null ? request.getFormat().name().toLowerCase() : "csv";
        return String.format("report_%s_%s.%s", reportType, timestamp, extension);
    }

    /**
     * Get media type for export format
     */
    private MediaType getMediaType(ReportExportRequest.ExportFormat format) {
        if (format == null) {
            return MediaType.TEXT_PLAIN;
        }
        return switch (format) {
            case CSV -> MediaType.parseMediaType("text/csv");
            case JSON -> MediaType.APPLICATION_JSON;
            case XLSX -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
    }
}
