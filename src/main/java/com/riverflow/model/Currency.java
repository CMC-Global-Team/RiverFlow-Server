package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing supported currencies in the system
 * Supports multi-currency functionality
 */
@Entity
@Table(name = "currencies", indexes = {
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    /**
     * ISO 4217 currency code (USD, EUR, VND, etc.)
     */
    @Column(nullable = false, unique = true, length = 3)
    private String code;
    
    /**
     * Full name of the currency
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * Currency symbol ($, €, ₫, etc.)
     */
    @Column(nullable = false, length = 10)
    private String symbol;
    
    /**
     * Number of decimal places for this currency
     * E.g., 2 for USD, 0 for JPY
     */
    @Column(name = "decimal_places", nullable = false)
    private Byte decimalPlaces = 2;
    
    /**
     * Whether this currency is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Display order in currency lists
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

