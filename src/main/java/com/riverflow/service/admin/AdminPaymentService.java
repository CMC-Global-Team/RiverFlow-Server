package com.riverflow.service.admin;

import com.riverflow.dto.admin.AdminPaymentResponse;
import com.riverflow.dto.admin.PaymentStatisticsResponse;
import com.riverflow.dto.admin.PaymentStatusUpdateRequest;
import com.riverflow.model.logging.ActivityLog;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import com.riverflow.service.logging.ActivityLoggingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Service for admin payment management operations
 */
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ActivityLoggingService activityLoggingService;

    /**
     * Get all payments with search, filter, sort, and pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentResponse> getAllPayments(
            String search,
            String status,
            String gateway,
            String transferType,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        // Build sort
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build specification for dynamic filtering
        Specification<PaymentTransaction> spec = buildPaymentSpecification(search, status, gateway, transferType,
                startDate, endDate);

        Page<PaymentTransaction> paymentPage = paymentTransactionRepository.findAll(spec, pageable);
        return paymentPage.map(this::mapToAdminPaymentResponse);
    }

    /**
     * Get payment by ID
     */
    @Transactional(readOnly = true)
    public AdminPaymentResponse getPaymentById(Long paymentId) {
        PaymentTransaction payment = paymentTransactionRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found with id: " + paymentId));
        return mapToAdminPaymentResponse(payment);
    }

    /**
     * Update payment status
     */
    @Transactional
    public AdminPaymentResponse updatePaymentStatus(Long paymentId, PaymentStatusUpdateRequest request,
            Long actorId, String actorEmail, String actorRole) {
        PaymentTransaction payment = paymentTransactionRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found with id: " + paymentId));

        String oldStatus = payment.getStatus() != null ? payment.getStatus().name() : "unknown";

        try {
            PaymentTransaction.TransactionStatus newStatus = PaymentTransaction.TransactionStatus
                    .valueOf(request.getStatus().toLowerCase());
            payment.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
        }

        paymentTransactionRepository.save(payment);

        // Log the action
        activityLoggingService.logPaymentManagementAction(
                actorId, actorEmail, actorRole,
                ActivityLog.Action.PAYMENT_STATUS_UPDATE.name(), paymentId,
                String.format("{\"oldStatus\":\"%s\",\"newStatus\":\"%s\"}", oldStatus, request.getStatus()));

        return mapToAdminPaymentResponse(payment);
    }

    /**
     * Get payment statistics (super admin only)
     */
    @Transactional(readOnly = true)
    public PaymentStatisticsResponse getPaymentStatistics() {
        // Get today's start
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        // Get week start (Monday)
        LocalDateTime weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1)
                .atStartOfDay();
        // Get month start
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return PaymentStatisticsResponse.builder()
                .totalPayments(paymentTransactionRepository.count())
                .totalAmount(paymentTransactionRepository.sumTotalAmount())
                .pendingCount(paymentTransactionRepository.countByStatus(PaymentTransaction.TransactionStatus.pending))
                .processedCount(
                        paymentTransactionRepository.countByStatus(PaymentTransaction.TransactionStatus.processed))
                .matchedCount(paymentTransactionRepository.countByStatus(PaymentTransaction.TransactionStatus.matched))
                .ignoredCount(paymentTransactionRepository.countByStatus(PaymentTransaction.TransactionStatus.ignored))
                .invalidCount(paymentTransactionRepository.countByStatus(PaymentTransaction.TransactionStatus.invalid))
                .todayPayments(paymentTransactionRepository.countByCreatedAtAfter(todayStart))
                .todayAmount(paymentTransactionRepository.sumAmountByCreatedAtAfter(todayStart))
                .weekPayments(paymentTransactionRepository.countByCreatedAtAfter(weekStart))
                .weekAmount(paymentTransactionRepository.sumAmountByCreatedAtAfter(weekStart))
                .monthPayments(paymentTransactionRepository.countByCreatedAtAfter(monthStart))
                .monthAmount(paymentTransactionRepository.sumAmountByCreatedAtAfter(monthStart))
                .build();
    }

    /**
     * Export payments as CSV
     */
    @Transactional(readOnly = true)
    public byte[] exportPaymentsAsCsv(String search, String status, String gateway,
            String transferType, LocalDate startDate, LocalDate endDate) {

        List<PaymentTransaction> payments = getPaymentsForExport(search, status, gateway, transferType, startDate,
                endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write BOM for Excel compatibility
            writer.write('\ufeff');
            // Write header
            writer.println(
                    "ID,External ID,Gateway,Transaction Date,Account Number,Code,Content,Transfer Type,Amount,Status,Created At,User Email,User Name");

            // Write data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (PaymentTransaction p : payments) {
                writer.printf("%d,%s,%s,%s,%s,%s,\"%s\",%s,%d,%s,%s,%s,%s%n",
                        p.getId(),
                        p.getExternalId() != null ? p.getExternalId() : "",
                        escapeCsv(p.getGateway()),
                        p.getTransactionDate() != null ? p.getTransactionDate().format(formatter) : "",
                        escapeCsv(p.getAccountNumber()),
                        escapeCsv(p.getCode()),
                        escapeCsv(p.getContent()),
                        p.getTransferType() != null ? p.getTransferType().name() : "",
                        p.getTransferAmount() != null ? p.getTransferAmount() : 0,
                        p.getStatus() != null ? p.getStatus().name() : "",
                        p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : "",
                        p.getUser() != null ? escapeCsv(p.getUser().getEmail()) : "",
                        p.getUser() != null ? escapeCsv(p.getUser().getFullName()) : "");
            }
        }
        return baos.toByteArray();
    }

    /**
     * Export payments as TXT
     */
    @Transactional(readOnly = true)
    public byte[] exportPaymentsAsTxt(String search, String status, String gateway,
            String transferType, LocalDate startDate, LocalDate endDate) {

        List<PaymentTransaction> payments = getPaymentsForExport(search, status, gateway, transferType, startDate,
                endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.println("=".repeat(100));
            writer.println("PAYMENT TRANSACTIONS REPORT");
            writer.println(
                    "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("Total Records: " + payments.size());
            writer.println("=".repeat(100));
            writer.println();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (PaymentTransaction p : payments) {
                writer.println("-".repeat(80));
                writer.printf("ID: %d | Code: %s | Status: %s%n",
                        p.getId(), p.getCode(), p.getStatus());
                writer.printf("Gateway: %s | Amount: %,d VND | Type: %s%n",
                        p.getGateway(), p.getTransferAmount(), p.getTransferType());
                writer.printf("Date: %s%n",
                        p.getTransactionDate() != null ? p.getTransactionDate().format(formatter) : "N/A");
                if (p.getUser() != null) {
                    writer.printf("User: %s (%s)%n", p.getUser().getFullName(), p.getUser().getEmail());
                }
                if (p.getContent() != null && !p.getContent().isEmpty()) {
                    writer.printf("Content: %s%n", p.getContent());
                }
                writer.println();
            }
            writer.println("=".repeat(100));
            writer.println("END OF REPORT");
        }
        return baos.toByteArray();
    }

    /**
     * Export payments as JSON
     */
    @Transactional(readOnly = true)
    public byte[] exportPaymentsAsJson(String search, String status, String gateway,
            String transferType, LocalDate startDate, LocalDate endDate) {

        List<PaymentTransaction> payments = getPaymentsForExport(search, status, gateway, transferType, startDate,
                endDate);
        List<AdminPaymentResponse> responses = payments.stream()
                .map(this::mapToAdminPaymentResponse)
                .toList();

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(responses);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export as JSON");
        }
    }

    /**
     * Get payments for export (no pagination)
     */
    private List<PaymentTransaction> getPaymentsForExport(String search, String status, String gateway,
            String transferType, LocalDate startDate, LocalDate endDate) {
        Specification<PaymentTransaction> spec = buildPaymentSpecification(search, status, gateway, transferType,
                startDate, endDate);
        return paymentTransactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Build payment specification for dynamic filtering
     */
    private Specification<PaymentTransaction> buildPaymentSpecification(
            String search, String status, String gateway, String transferType,
            LocalDate startDate, LocalDate endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by code, content, or user email
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("code")), searchPattern);
                Predicate contentPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("content")), searchPattern);
                predicates.add(criteriaBuilder.or(codePredicate, contentPredicate));
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    PaymentTransaction.TransactionStatus transactionStatus = PaymentTransaction.TransactionStatus
                            .valueOf(status.toLowerCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), transactionStatus));
                } catch (IllegalArgumentException ignored) {
                    // Invalid status, ignore filter
                }
            }

            // Filter by gateway
            if (gateway != null && !gateway.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("gateway")), gateway.toLowerCase()));
            }

            // Filter by transfer type
            if (transferType != null && !transferType.trim().isEmpty()) {
                try {
                    PaymentTransaction.TransferType type = PaymentTransaction.TransferType
                            .valueOf(transferType.toLowerCase());
                    predicates.add(criteriaBuilder.equal(root.get("transferType"), type));
                } catch (IllegalArgumentException ignored) {
                    // Invalid transfer type, ignore filter
                }
            }

            // Filter by date range
            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
            }
            if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Map PaymentTransaction entity to AdminPaymentResponse DTO
     */
    private AdminPaymentResponse mapToAdminPaymentResponse(PaymentTransaction payment) {
        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .externalId(payment.getExternalId())
                .gateway(payment.getGateway())
                .transactionDate(payment.getTransactionDate())
                .accountNumber(payment.getAccountNumber())
                .code(payment.getCode())
                .content(payment.getContent())
                .transferType(payment.getTransferType() != null ? payment.getTransferType().name() : null)
                .transferAmount(payment.getTransferAmount())
                .accumulated(payment.getAccumulated())
                .subAccount(payment.getSubAccount())
                .referenceCode(payment.getReferenceCode())
                .description(payment.getDescription())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .createdAt(payment.getCreatedAt())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userEmail(payment.getUser() != null ? payment.getUser().getEmail() : null)
                .userFullName(payment.getUser() != null ? payment.getUser().getFullName() : null)
                .matchedRequestId(payment.getMatchedRequest() != null ? payment.getMatchedRequest().getId() : null)
                .matchedRequestCode(payment.getMatchedRequest() != null ? payment.getMatchedRequest().getCode() : null)
                .build();
    }

    /**
     * Escape CSV value
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }
}
