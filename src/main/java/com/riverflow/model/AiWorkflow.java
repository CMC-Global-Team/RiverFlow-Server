package com.riverflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing AI workflow templates
 */
@Entity
@Table(name = "ai_workflows", indexes = {
    @Index(name = "idx_slug", columnList = "slug"),
    @Index(name = "idx_category", columnList = "category_id"),
    @Index(name = "idx_active", columnList = "is_active"),
    @Index(name = "idx_featured", columnList = "is_featured"),
    @Index(name = "idx_usage", columnList = "usage_count"),
    @Index(name = "idx_rating", columnList = "rating_average")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWorkflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AiWorkflowCategory category;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * AI prompt template with variables
     */
    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;
    
    /**
     * Schema for required inputs: {"field": "type"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema", columnDefinition = "JSON")
    private String inputSchema;
    
    /**
     * Output format
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", length = 20)
    private OutputFormat outputFormat = OutputFormat.mindmap;
    
    /**
     * Array of tags for search
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private String tags;
    
    /**
     * Difficulty level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    private DifficultyLevel difficultyLevel = DifficultyLevel.beginner;
    
    /**
     * Estimated time in minutes
     */
    @Column(name = "estimated_time")
    private Integer estimatedTime;
    
    /**
     * Usage tracking
     */
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    
    @Column(name = "rating_average", precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;
    
    @Column(name = "rating_count")
    private Integer ratingCount = 0;
    
    /**
     * Status
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Enums
    
    public enum OutputFormat {
        text,
        json,
        mindmap,
        list
    }
    
    public enum DifficultyLevel {
        beginner,
        intermediate,
        advanced
    }
}

