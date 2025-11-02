package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing exchange rates between currencies
 * All rates are relative to a base currency (typically USD)
 */
@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_currencies", columnList = "from_currency_id, to_currency_id"),
    @Index(name = "idx_active", columnList = "is_active, valid_from, valid_until"),
    @Index(name = "idx_valid_dates", columnList = "valid_from, valid_until")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_active_rate", columnNames = {"from_currency_id", "to_currency_id", "is_active"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Source currency for conversion
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exchange_rate_from_currency"))
    @NotNull(message = "From currency is required")
    private Currency fromCurrency;
    
    /**
     * Target currency for conversion
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exchange_rate_to_currency"))
    @NotNull(message = "To currency is required")
    private Currency toCurrency;
    
    /**
     * Exchange rate: 1 from_currency = rate * to_currency
     */
    @Column(nullable = false, precision = 20, scale = 8)
    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be positive")
    private BigDecimal rate;
    
    /**
     * Source of the exchange rate (API provider, manual entry, etc.)
     */
    @Column(length = 100)
    private String source;
    
    /**
     * Start date when this rate becomes valid
     */
    @Column(name = "valid_from", nullable = false)
    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom = LocalDateTime.now();
    
    /**
     * End date when this rate expires (null means currently active)
     */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    
    /**
     * Whether this rate is currently active
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

