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
 * Entity representing service packages (Free, Pro, Enterprise, etc.)
 * Created and managed by administrators
 */
@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_slug", columnList = "slug"),
    @Index(name = "idx_active", columnList = "is_active"),
    @Index(name = "idx_base_currency", columnList = "base_currency_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Package {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Package name (e.g., "Free", "Pro", "Enterprise")
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Package name is required")
    private String name;
    
    /**
     * Package description
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * URL-friendly identifier
     */
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Slug is required")
    private String slug;
    
    // Pricing
    
    /**
     * Base price in base currency (for reference)
     */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice = BigDecimal.ZERO;
    
    /**
     * Base currency (usually USD)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false, foreignKey = @ForeignKey(name = "fk_package_base_currency"))
    @NotNull(message = "Base currency is required")
    private Currency baseCurrency;
    
    /**
     * Package duration in days
     */
    @Column(name = "duration_days", nullable = false)
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays = 30;
    
    // Limits
    
    /**
     * Maximum number of mindmaps (0 = unlimited)
     */
    @Column(name = "max_mindmaps", nullable = false)
    @NotNull(message = "Max mindmaps is required")
    @Min(value = 0, message = "Max mindmaps must be non-negative")
    private Integer maxMindmaps = 10;
    
    /**
     * Maximum number of collaborators (0 = unlimited)
     */
    @Column(name = "max_collaborators", nullable = false)
    @NotNull(message = "Max collaborators is required")
    @Min(value = 0, message = "Max collaborators must be non-negative")
    private Integer maxCollaborators = 5;
    
    /**
     * Storage limit in MB (0 = unlimited)
     */
    @Column(name = "max_storage_mb", nullable = false)
    @NotNull(message = "Max storage is required")
    @Min(value = 0, message = "Max storage must be non-negative")
    private Integer maxStorageMb = 100;
    
    // Features
    
    /**
     * Package features stored as JSON
     * Example: {"real_time": true, "export_pdf": true, "templates": true}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> features;
    
    // Status
    
    /**
     * Whether this package is currently active and available
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Display order on pricing page
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

