package com.riverflow.controller.admin;

import com.riverflow.dto.logging.ActivityLogResponse;
import com.riverflow.dto.logging.LogStatisticsResponse;
import com.riverflow.service.logging.ActivityLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * REST Controller for super admin logging access.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/super-admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminLoggingController {

    private final ActivityLoggingService activityLoggingService;

    /**
     * Get logs with search, filter, and pagination
     * GET
     * /api/super-admin/logs?search=&category=&action=&actorRole=&startDate=&endDate=&page=0&size=50&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> getLogs(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<ActivityLogResponse> logs = activityLoggingService.getLogs(
                search, category, action, actorRole, startDateTime, endDateTime, page, size, sortDir);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get log by ID
     * GET /api/super-admin/logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityLogResponse> getLogById(@PathVariable String id) {
        ActivityLogResponse log = activityLoggingService.getLogById(id);
        return ResponseEntity.ok(log);
    }

    /**
     * Get log statistics
     * GET /api/super-admin/logs/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<LogStatisticsResponse> getStatistics() {
        LogStatisticsResponse statistics = activityLoggingService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get available categories
     * GET /api/super-admin/logs/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<String[]> getCategories() {
        String[] categories = new String[] {
                "USER_MANAGEMENT",
                "PAYMENT_MANAGEMENT",
                "PAYMENT",
                "AUTHENTICATION",
                "SYSTEM"
        };
        return ResponseEntity.ok(categories);
    }

    /**
     * Get available actions
     * GET /api/super-admin/logs/actions
     */
    @GetMapping("/actions")
    public ResponseEntity<String[]> getActions() {
        String[] actions = new String[] {
                "USER_CREATE",
                "USER_UPDATE",
                "USER_DELETE",
                "USER_SOFT_DELETE",
                "USER_HARD_DELETE",
                "USER_RESTORE",
                "CREDIT_UPDATE",
                "PASSWORD_CHANGE",
                "PAYMENT_SUCCESS",
                "PAYMENT_FAILED",
                "PAYMENT_STATUS_UPDATE",
                "LOGIN",
                "LOGOUT",
                "SYSTEM_CONFIG_UPDATE"
        };
        return ResponseEntity.ok(actions);
    }
}
