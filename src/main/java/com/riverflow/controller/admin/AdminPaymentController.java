package com.riverflow.controller.admin;

import com.riverflow.dto.admin.AdminPaymentResponse;
import com.riverflow.dto.admin.PaymentStatisticsResponse;
import com.riverflow.dto.admin.PaymentStatusUpdateRequest;
import com.riverflow.service.admin.AdminPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for admin payment management
 * All endpoints require ADMIN or SUPER_ADMIN role
 * Statistics and export endpoints require SUPER_ADMIN role
 */
@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    /**
     * Get all payments with pagination, search, filter, and sort
     * GET
     * /api/admin/payments?page=0&size=10&search=code&status=pending&gateway=sepay&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<Page<AdminPaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminPaymentResponse> payments = adminPaymentService.getAllPayments(
                search, status, gateway, transferType, startDate, endDate, sortBy, sortDir, page, size);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment by ID
     * GET /api/admin/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminPaymentResponse> getPaymentById(@PathVariable Long id) {
        AdminPaymentResponse payment = adminPaymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * Update payment status
     * PUT /api/admin/payments/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<AdminPaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        AdminPaymentResponse payment = adminPaymentService.updatePaymentStatus(id, request);
        return ResponseEntity.ok(payment);
    }

    /**
     * Get payment statistics (super admin only)
     * GET /api/admin/payments/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentStatisticsResponse> getPaymentStatistics() {
        PaymentStatisticsResponse statistics = adminPaymentService.getPaymentStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Export payments as CSV (super admin only)
     * GET /api/admin/payments/export/csv
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] csvData = adminPaymentService.exportPaymentsAsCsv(search, status, gateway, transferType, startDate,
                endDate);

        String filename = "payments_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Export payments as TXT (super admin only)
     * GET /api/admin/payments/export/txt
     */
    @GetMapping("/export/txt")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsTxt(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] txtData = adminPaymentService.exportPaymentsAsTxt(search, status, gateway, transferType, startDate,
                endDate);

        String filename = "payments_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(txtData);
    }

    /**
     * Export payments as JSON (super admin only)
     * GET /api/admin/payments/export/json
     */
    @GetMapping("/export/json")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsJson(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] jsonData = adminPaymentService.exportPaymentsAsJson(search, status, gateway, transferType, startDate,
                endDate);

        String filename = "payments_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonData);
    }
}
