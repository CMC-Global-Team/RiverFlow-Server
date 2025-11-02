package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing payment transactions
 * Supports multiple payment methods: QR Banking, PayPal, Stripe, etc.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_status", columnList = "payment_status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_currency", columnList = "currency_id"),
    @Index(name = "idx_payments_completed_at", columnList = "completed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User making the payment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_user"))
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Package being purchased
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_package"))
    @NotNull(message = "Package is required")
    private Package packageEntity;
    
    // Payment details
    
    /**
     * Payment amount
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;
    
    /**
     * Currency used for payment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_currency"))
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    /**
     * Original amount if currency conversion occurred
     */
    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;
    
    /**
     * Original currency before conversion
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_currency_id", foreignKey = @ForeignKey(name = "fk_payment_original_currency"))
    private Currency originalCurrency;
    
    /**
     * Exchange rate used at time of payment
     */
    @Column(name = "exchange_rate", precision = 20, scale = 8)
    private BigDecimal exchangeRate;
    
    /**
     * Payment method used
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    /**
     * Current payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    // Transaction info
    
    /**
     * External transaction ID from payment gateway
     */
    @Column(name = "transaction_id")
    private String transactionId;
    
    /**
     * Full response from payment gateway (stored as JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_gateway_response", columnDefinition = "JSON")
    private Map<String, Object> paymentGatewayResponse;
    
    // QR Banking specific fields
    
    /**
     * URL to generated QR code for payment
     */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;
    
    /**
     * Bank transaction reference
     */
    @Column(name = "bank_transaction_ref")
    private String bankTransactionRef;
    
    // Payment metadata
    
    /**
     * Additional payment information (stored as JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> metadata;
    
    // Admin actions
    
    /**
     * Admin who verified this payment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", foreignKey = @ForeignKey(name = "fk_payment_verified_by"))
    private User verifiedBy;
    
    /**
     * Timestamp when payment was verified
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * Admin notes about the payment
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Timestamps
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Timestamp when payment was completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Enums
    
    public enum PaymentMethod {
        QR_BANKING,
        PAYPAL,
        STRIPE,
        MANUAL
    }
    
    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}

