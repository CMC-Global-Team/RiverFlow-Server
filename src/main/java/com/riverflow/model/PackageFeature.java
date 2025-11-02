package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity defining available features that can be assigned to packages
 */
@Entity
@Table(name = "package_features", indexes = {
    @Index(name = "idx_feature_key", columnList = "feature_key"),
    @Index(name = "idx_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageFeature {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique feature key (e.g., "real_time", "export_pdf", "ai_suggestions")
     */
    @Column(name = "feature_key", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Feature key is required")
    private String featureKey;
    
    /**
     * Display name of the feature
     */
    @Column(name = "feature_name", nullable = false)
    @NotBlank(message = "Feature name is required")
    private String featureName;
    
    /**
     * Feature description
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Feature category (e.g., "collaboration", "export", "advanced")
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Category is required")
    private String category = "general";
    
    /**
     * Whether this feature is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

