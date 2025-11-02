package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing package prices in different currencies
 * Supports multi-currency pricing and promotional pricing
 */
@Entity
@Table(name = "package_prices", indexes = {
    @Index(name = "idx_package_currency", columnList = "package_id, currency_id"),
    @Index(name = "idx_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_package_currency", columnNames = {"package_id", "currency_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Package this price belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, foreignKey = @ForeignKey(name = "fk_package_price_package"))
    @NotNull(message = "Package is required")
    private Package packageEntity;
    
    /**
     * Currency for this price
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_package_price_currency"))
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    /**
     * Regular price in the specified currency
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;
    
    // Promotional pricing
    
    /**
     * Promotional (discounted) price
     */
    @Column(name = "promotional_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Promotional price must be non-negative")
    private BigDecimal promotionalPrice;
    
    /**
     * Promotion start date
     */
    @Column(name = "promotion_start_date")
    private LocalDateTime promotionStartDate;
    
    /**
     * Promotion end date
     */
    @Column(name = "promotion_end_date")
    private LocalDateTime promotionEndDate;
    
    /**
     * Whether this price is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

