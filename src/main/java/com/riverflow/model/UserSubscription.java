package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing user subscriptions to packages
 */
@Entity
@Table(name = "user_subscriptions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_package_id", columnList = "package_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_dates", columnList = "start_date, end_date"),
    @Index(name = "idx_subscriptions_end_date", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who owns this subscription
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_user"))
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Package being subscribed to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subscription_package"))
    @NotNull(message = "Package is required")
    private Package packageEntity;
    
    /**
     * Reference to the payment that activated this subscription
     */
    @Column(name = "payment_id")
    private Long paymentId;
    
    // Subscription period
    
    /**
     * Subscription start date
     */
    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    
    /**
     * Subscription end date
     */
    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
    
    // Status
    
    /**
     * Subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;
    
    /**
     * Whether subscription should auto-renew
     */
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;
    
    // Cancellation
    
    /**
     * Timestamp when subscription was cancelled
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    /**
     * Reason for cancellation
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Enums
    
    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        PENDING
    }
}

